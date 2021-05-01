# Data storage module

## Join protocol

- **Arguments:** gateway node
- **Returns:** -

The Join protocol allows a fresh node to join the system.

On join, the joining node $r$ must perform three steps:

1. Initialize its fingers table and predecessor
2. Update the predecessors and fingers tables of other nodes
3. Transfer objects from its successor to itself

The joining node needs to know the socket address of at least one node that is already in the chord; we will call it the gateway node $g$, as it is the joining node's gateway into the chord while the joining node knows nothing about that.

### Initialize fingers table and predecessor

To initialize its fingers table, $r$ asynchronously makes the required queries to $g$.

#### Build fingers table

To build its fingers table, $r$ asks to $g$ what is the successor of $r' = r + 2^k$, for all $values of $0 ≤ k < m$, using several `GETSUCCESSOR` messages directed at $g$.

The joining node can asynchronously make all `GETSUCCESSOR` requests to $g$, but that yields $O(m \log N)$ time complexity, while [@stoica2001] states that, if requests are made sequentially, some trivial tests can be made to skip asking about some fingers.

TODO | what's better: full asynchronous, or sequential questioning and tests, with less network usage but probably slower?

#### Get predecessor

It is enough for $r$ to run the GetPredecessor protocol, as it has already built its fingers table; it can merely ask its successor about its predecessor (because the successor of $r$ was not yet told that $r$ joined the network, it will return what it thinks is its predecessor, when actually it is the predecessor of $r$).

### Update other nodes

The joining node $r$ needs to notify other nodes in the system to update the information they have about the system.
Node $r$ must namely notify its successor that $r$ is its new predecessor, using protocol UpdatePredecessor.
It must also notify some of the other nodes in the system to update their fingers tables, by running one instance of the UpdateFingers protocol.

### MoveKeys protocol

- **Arguments:** -
- **Returns:** -

To move the keys from its successor to itself, the joining node sends a message to its successor (which has keys that the fresh peer must now store instead of its successor), with format

```
MOVEKEYS <SenderId> <IP>:<Port>
```

This message indicates that there is a new node in the system, and as such the node that receives this message must do the following for each datapiece it is storing with key less than or equal to `<SenderId>`:

1. Load the datapiece from secondary memory.
2. Start the `Delete` protocol for that datapiece, directed at itself.
3. Start the `Put` protocol for that datapiece, directed at the peer that sent the `MOVEKEYS` message, with that same chunk.

## PutSystem protocol

- **Arguments:** current peer, UUID, data
- **Returns:** -

```
PUT <SenderId> <UUID><LF><Body>
```

Instructs a peer receiving it that it should store a chunk with a certain UUID, using its successors if necessary. Upon receiving this message, it should respond with a single integer: 0 if it was successful, another number otherwise (with 1 referring to a generic error).

If a peer receives this message with its ID in the `<PeerId>` field, it fails.

Upon receiving this message, a peer $p$ should perform the following actions:

1. If $p$ has not stored that chunk and does not have space for another chunk:
   1. It registers locally that the chunk is being stored in its successor.
   2. It redirects the message to its successor $q = successor(p)$.
   3. If $q$'s answer fails, $p$ removes its local registry saying that the chunk was stored in its successor.
   4. $p$ answers the original message with the exact content of the response it got from $q$.
2. If $p$ has already stored that chunk:
   1. It succeeds.
3. If $p$ has not stored that chunk but has a pointer to its successor reporting that it might have stored:
   1. If it has space for that chunk:
      1. It stores the chunk locally.
      2. It asynchronously sends to its successor $q = successor(p)$ a `DELETE` message to delete that chunk.
      3. It replies successfully
   2. If it does not have space for that chunk:
      1. It redirects the message to its successor $q = successor(p)$.
      2. It replies to the original message with whatever $q$ replied.
4. If $p$ has not yet stored that chunk and has available space:
   1. It stores that chunk;
   2. It succeeds.

## DeleteSystem protocol

- **Arguments:** current peer
- **Returns:** -

We locate all replicas of chunks of said file, and start the DeleteData protocol for those replicas.

We also have to take into account the scenario where a peer is down; say a peer is shutdown and it was storing a certain replica, and afterwards it is requested for that chunk to be deleted:
- If the peer ran the chord leaving protocol, then on shutdown it contains no replicas at all, and all the replicas it had were transferred to another peer; in this case we do not have to worry, as all replicas of a chunk are active in the system at all times, and upon sending a DELETE message it is guaranteed that all replicas will be deleted.
- If the peer did not run the chord leaving protocol, then the user metadata file must place that file in a special **trash** category, listing all replicas that have not yet been deleted.

## Hello protocol

- **Arguments:** current peer, successor
- **Returns:** -

The Hello protocol allows a peer to notify all nodes in the network that it just joined the network. This is particularly useful if said node exited the chord without notifying other peers and without moving its keys to its predecessor; the network can still answer queries because several replicas are probably stored in different peers (they might also end up in the same peer that is shutdown, although it is highly unlikely), but when the shutdown peer comes back online it might have issues with not having deleted files that were deleted while it was offline.

When called, the HELLO protocol creates a HELLO message with format

```
HELLO <SenderId>
```

and sends it to the current node's successor, and closes the socket.

When a peer receives a HELLO message it should:
- Close the incoming socket
- Open a new socket to its successor
- Forward the message to its successor
- Close the socket to the successor.

When a peer receives a HELLO message with its own `<SenderId>` it ignores the message.

## GetSystem protocol

- **Arguments:** current peer, UUID
- **Returns:** data

The Get protocol allows a peer to get 

## Leave protocol

TODO
