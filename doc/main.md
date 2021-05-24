## Main module

This module allows the main operations a peer needs to perform:

- Authenticate with a username and password
- Backup a file with a certain replication degree
- Delete a file (including all replicas of each chunk)
- Restore a file

Each user has a username and a password, and can use the system as a distributed remote storage area, where its files are only accessible to itself, and a user can log-in from any peer to perform any operation (backup/restore/delete a file or delete its account). A username may not contain characters forbidden in Windows or Linux file names (`<`, `>`, `:`, `"`, `/`, `\`, `|`, `?`, `*`).

User information is stored in a special *user metadata file*. Each file belongs to one user (including its own user metadata file).

Each file has a replication degree $D$ and a user-defined path $p$; this defines the location of the file inside the user storage area.

A file has an ID $f$ calculated from the user $u$ that owns it (its *owner*) and $p$ ($f = concatenate(u, \texttt{'/'}, p))$. The ID of the $n$-th chunk of a file with ID $f$ is $c = concatenate(f, \texttt{'-'}, n)$. Each replication of a chunk with ID $c$ is called a *replica*, and each replica is numbered with a replication index $d$ ($0 ≤ d < D$); a replica has ID $r = concatenate(c, \texttt{'-'}, d)$. Thus, the format for a file name is either:

- `<User>/<Path>-<ChunkNo>-<ReplicationIdx>` for a regular file.
- `<User>-<ChunkNo>-<ReplicationIdx>` for a user metadata file.

A replica in the main module always corresponds to a datapiece in lower-level protocols, and the key $k$ corresponding to a certain replica ID $r$ is $k = h(r)$ where $h$ is a hashing function. In our case, we decided to use as hashing function the SHA-256 algorithm (which outputs a $\SI{256}{\bit}$ hash) and use only its first $\SI{64}{\bit}$ to fit it into a Java `long`. Bear in mind that this hash is only needed to find the destination using the chord protocol, not to store the files; these are stored locally using the path specified by the user, so it is guaranteed that, even if two files have the same hash, they will not overwrite each other.

### Authentication protocol

- **Arguments:** username, password
- **Returns:** -

For each user $u$, the system stores a file with path $u$ with all the metadata about that user:

- Username
- Password (only the password hash is stored)
- A table with the complete list of all files backed-up by that user (the ID of that table is the file path), having for each file its:
  - Path
  - Number of chunks
  - Replication degree

This allows all system data to be fully distributed. This file is stored, loaded, edited and deleted using the BackupFile, DeleteFile and RestoreFile protocols with a replication degree of 10; this means the user metadata file also benefits from redundancy and does not need to use lower-level protocols, including having to calculate the key from its ID. Ideally, this information would be stored in a distributed database, but to limit complexity we decided to use the same file backup system to store user metadata.

Upon starting the Authentication protocol, the peer sends an `AUTHENTICATE` message to any peer (as of now, its successor, because it can always be found in constant time).

#### `AUTHENTICATE` message

| **Request**                          | | **Response** |
|--------------------------------------|-|--------------|
| `AUTHENTICATE <Username> <Password>` | | `<Metadata>` |

Upon receiving this message, a peer starts its `AUTHENTICATE` message processor:

1. Calls the RestoreFile protocol for the user's metadata file
2. If the RestoreFile protocol fails
   1. A new user metadata file is created for that user
   2. The new user metadata file is stored to the system using an instance of BackupFile protocol
   3. Recall the message processor
3. If the RestoreFile protocol succeeds
   1. If the user metadata file is broken, respond with `1` (`BROKEN`)
   2. If the stored hashed password is different from the hash of the password in the `AUTHENTICATE` message, respond with `2` (`UNAUTHORIZED`)
   3. If the stored hashed password is equal to the hash of the password in the `AUTHENTICATE` message, respond in format `0<Body>` (`SUCCESS`) where `<Body>` contains the user metadata file's contents

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
