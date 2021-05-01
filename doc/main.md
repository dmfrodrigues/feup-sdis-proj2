## Main module

### Authentication protocol

The system allows an account-based interaction, where each user has a username and a password.

For each user with username $u$, the system stores a file with ID $"user/".u$ with all the metadata about that user:
- Username
- Password
- A table with the complete list of all files backed-up by that user (the ID of that table is the file path), having for each file its:
  - Path
  - ID (determined from the hash of whole file contents)
  - Replication degree
  - List of chunks, for each chunk having its:
    - Chunk number
    - List of peers that have reported to be replicating that chunk
- A table with the list of all files that were requested to be deleted, but the system perceived it could not delete all replicas of chunks of that file, because at least one node was offline; each entry has:
  - The ID of the infringing peer
  - The list of chunks it has not yet deleted

This allows all system data to be fully distributed. This file is stored at $successor(h(u))$ with a replication degree of 10.

When starting a peer, you must either register or login. Upon registering, you must pick a unique username. Upon login, the password will be checked, and the fresh peer will only start successfully if the peer that has the user information confirms the password is valid, and sends the user metadata file to the fresh peer.

One can also delete an account, by entering the right credentials; all files for that user are deleted from the system.

### Backup protocol

Divides a file into several chunks, calculates each chunk replica's UUID using the chunk ID and the replication index $d \in [0, D)$, and runs the PutChunk protocol for each replica.

We will use the chord protocol, where the network is represented as a circle, in which a consistent hash with $m$ bits is used, and a peer at position $r$ knows its predecessor, and contains a *fingers table* of indices $m$ where for each $0 ≤ k < m$ it knows the successor of $r+2^k$ (including its own next peer, which is in table index $k=0$).

This table is created when the peer joins the chord, and updated by other peers when another peer joins or leaves the chord.

The peer responsible for a key is its *successor*, where $successor(i)$ is the successor of peer $i$.

### DeleteFile protocol

By knowing the file ID, the number of chunks and the replication degree of the file, the initiator peer can calculate all UUIDs of the replicas of all chunks; it executes the Delete protocol for each of those replicas.

### Restore protocol

To locate a chunk with ID $i$, we use the user metadata, and iterate over the list of peers that have reported to be replicating that chunk, until we find one that is online. If no replica is found, the protocol aborts.

### Reclaim protocol

TODO
