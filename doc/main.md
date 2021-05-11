## Main module

Each file has a replication degree $D$. A file has an ID $f$ calculated from its file contents and with the "f/" prefix ($f = concatenate("f/", h(filecontents))$, where hashing function $h$ needs not be the same as in lower-level protocols).

The ID of the $n$-th chunk of a file with ID $f$ is $c = concatenate(f, "-", n)$.

Each replication of a chunk with ID $c$ is called a *replica*, and each replica is numbered with a replication index $d$ ($0 ≤ d < D$); a replica has ID $r = concatenate(c, "-", d)$.

A replica in the main module always corresponds to a datapiece in lower-level protocols, and the key $k$ corresponding to a certain replica ID $r$ is $k = h(r)$.

This module allows the main operations a peer needs to perform:

- Authenticate with a username and password
- Backup a file with a certain replication degree
- Delete a file (including all replicas of each chunk)
- Restore a file

### Authentication protocol

- **Arguments:** username, password
- **Returns:** -

The system allows an account-based interaction, where each user has a username and a password.

For each user with username $u$, the system stores a file with ID $concatenate("u/", u)$ with all the metadata about that user:

- Username
- Password (hashed with some hash function)
- A table with the complete list of all files backed-up by that user (the ID of that table is the file path), having for each file its:
  - Path
  - ID
  - Number of chunks
  - Replication degree

This allows all system data to be fully distributed. This file is stored, loaded, edited and deleted using the BackupFile, DeleteFile and RestoreFile protocols with a replication degree of 10; this means the user metadata file also benefits from redundancy and does not need to use lower-level protocols, including having to calculate the key from its ID. Ideally, this information would be stored in a distributed database, but to limit complexity we decided to use the same file backup system to store user metadata.

Both when registering or logging-in, the peer calls the same Authenticate protocol, where the new peer sends to a known peer that is part of the system (the gateway peer) a message with format

```
AUTHENTICATE <Username> <Password>
```

where `<Password>` is in plaintext.

Upon receiving this message, a peer calls the RestoreFile protocol for the corresponding user's metadata file:

1. If the RestoreFile protocol fails
   1. A new user metadata file is created for that user
   2. The peer responds in format `CREATED<LF><Body>` where `<Body>` contains the user metadata file's contents
2. If the RestoreFile protocol succeeds
   1. If the stored hashed password is equal to the hash of the password in the `AUTHENTICATE` message
      1. The peer responds in format `OK<LF><Body>` where `<Body>` contains the user metadata file's contents
   2. If the stored hashed password is different from the hash of the password in the `AUTHENTICATE` message
      1. The peer responds with `UNAUTHORIZED`

### BackupFile protocol

- **Arguments:** file path
- **Returns:** -

If a file with the same user and path is already being stored in the system, it is deleted using the DeleteFile protocol.

The peer first notifies the node that is storing the user metadata file to add said filename to the user metadata, using the message

```
ENLISTFILE <Username> <FilePath> <FileID> <NumChunks> <ReplicationDeg>
```

to tell said node to add it to the list of files the user backed-up.

Then the BackupFile protocol divides the file into several chunks, each with at most 64KB (64000B), calculates each chunk replica's UUID using the chunk ID and the replication index $d \in [0, D)$, and runs the PutChunk protocol for each replica.

### DeleteFile protocol

- **Arguments:** file path
- **Returns:** -

To delete a file, the peer first informs the node storing the user metadata file to remove said filename from the user metadata, using the message

```
DELISTFILE <Username> <FilePath>
```

to tell said node to remove it from the list of files the user backed-up.

Then the DeleteFile protocol goes through each chunk, and each replica, and calls the DeleteSystem protocol for every replica of every chunk of that file.

### RestoreFile protocol

- **Arguments:** file path
- **Returns:** -

The peer consults the user metadata file, and finds how many chunks the file is divided into and the replication degree $D$; then:

1. For each chunk $c$:
   1. Initialize a list of not-found replicas $L$
   2. For each replication index $d$ ($0 ≤ d < D$)
      1. If the GetSystem protocol succeeds
         1. Store it
         2. Continue
      2. Store the replica ID $r = concatenate(c,"-",d)$ in list $L$
   3. If a replica was not found:
      1. Exit with error
   4. For each replica not found:
      1. Run the PutSystem protocol for that replica, using another replica's contents

In the end, we just need to assemble the chunks.

### DeleteAccount protocol

- **Arguments:** -
- **Returns:** -

The DeleteAccount protocol allows a peer to delete its account, including all files tracked by that account. The account to be deleted is assumed to be the one the user logged-in.

When calling the DeleteAccount protocol, the peer sends a `BLOCKACCOUNT` message to avoid any further operations on the account, and waits to get the final version of the user metadata file.

The peer then starts by asynchronously calling DeleteFile protocols for each of the deleted files. After they all succeed, the peer calls a DeleteFile protocol to delete all the 10 replicas.

#### `BLOCKACCOUNT` message

This protocol serves the purpose of blocking an account, meaning no more files can be manually added, removed or restored from this account. It mostly serves the purpose of stabilizing the user metadata file before deleting all files of the account.

When calling the BlockAccount protocol, the peer finds the node that is storing the user metadata file, and sends it a message with contents:

```
BLOCKACCOUNT <Username>
```

On receiving a `BLOCKACCOUNT` message, if the account does not exist then it returns `NOTFOUND`. If the account exists and is already blocked, then some other peer is already deleting the account so it sends a `NOTFOUND` as well. If the account exists and is not blocked, the peer sets the account as blocked and returns in format `BLOCKED<LF><Body>` where `<Body>` is the contents of the user metadata file.
