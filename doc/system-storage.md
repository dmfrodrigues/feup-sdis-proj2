## System storage module

Supports the same operations as the data storage module (add, retrieve and delete datapieces) except when called the PutSystem, GetSystem and DeleteSystem protocols do not require to know where the datapieces should be stored according to their keys, and these protocols can be called from any node (while data storage protocols could only be called from the node it was supposed to apply).

DeleteSystem also handles the case where a peer that is essential to running the Delete protocol is down, using the Hello protocol to realise a node is online and retry to delete the datapiece.

It additionally supports protocols to:

- Join the system in an orderly fashion
- Say hello to all nodes in the system
- Change maximum allowed storage space for the current node
- Leave the system in an orderly fashion

### PutSystem protocol

- **Arguments:** key, data
- **Returns:** -

When a node $r$ calls the PutSystem protocol, it is telling the system it simply intends to store the datapiece somewhere in the system.

The PutSystem protocol consists of calling the GetSuccessor protocol to find the successor $s$ of the datapiece key, and then send a `PUT` message to $s$ with `<NodeKey>`$= s$ and the corresponding datapiece key and data.

### DeleteSystem protocol

- **Arguments:** key
- **Returns:** -

When a node $r$ calls the DeleteSystem protocol, it is telling the system it simply intends to delete the datapiece from wherever it is in the system.

The DeleteSystem protocol consists of calling the GetSuccessor protocol to find the successor $s$ of the datapiece key, and then send a `DELETE` message to $s$.

### GetSystem protocol

- **Arguments:** key
- **Returns:** data

When a node $r$ calls the GetSystem protocol, it is telling the system it simply wants the data identified with a certain key from wherever the system stored it.

The GetSystem protocol consists of calling the GetSuccessor protocol to find the successor $s$ of the datapiece key, and then send a `GET` message to $s$.

### Join protocol

- **Arguments:** gateway node
- **Returns:** -

The Join protocol allows a fresh node to join the system.

On join, the joining node $r$ must perform three steps:

1. Initialize its fingers table and predecessor
2. Update the predecessors and fingers tables of other nodes
3. Transfer objects from its successor to itself

The joining node needs to know the socket address of at least one node that is already in the chord; we will call it the gateway node $g$, as it is the joining node's gateway into the chord while the joining node knows nothing about that.

#### Initialize fingers table and predecessor

##### Build fingers table

To build its fingers table, $r$ asks to $g$ what is the successor of $k = r + 2^i$, for all values of $0 ≤ i < m$, using several `GETSUCCESSOR` messages directed at $g$.

The joining node can asynchronously make all `GETSUCCESSOR` requests to $g$, but that yields $O(m \log N)$ time complexity, while [@stoica2001] states that, if requests are made sequentially, some trivial tests can be made to skip asking about some fingers.

TODO | what's better: full asynchronous, or sequential questioning and tests, with less network usage but probably slower?

##### Get predecessor

It is enough for $r$ to send a `GETPREDECESSOR` message to its newly-found successor, as it has already built its fingers table so it knows it successor in $O(1)$; it can merely ask its successor about its predecessor (because the successor of $r$ was not yet told that $r$ joined the network, it will return what it thinks is its predecessor, when actually it is the predecessor of $r$).

#### Update other nodes

The joining node $r$ needs to notify other nodes in the system to update the information they have about the system.
Node $r$ must namely notify its successor that $r$ is its new predecessor, using protocol SetPredecessor.
It must also notify some of the other nodes in the system to update their fingers tables with its own key and socket address, by running one instance of the FingersAdd protocol.

#### MoveKeys protocol

- **Arguments:** -
- **Returns:** -

To move the keys from its successor $s$ to itself, the joining node $r$ sends a message to $s$ (which has keys that the joining node must now store instead of its successor), with format

```
MOVEKEYS <SenderId> <IP>:<Port>
```

This message tells $s$ that there is a new node $r$ in the system, and as such the node that receives this message must do the following for each datapiece it is storing with key $k$ such that $distance(k, r) < distance(k, s)$:

1. Load the datapiece from secondary memory.
2. Start the Delete protocol for that datapiece.
3. Start the PutSystem protocol for that datapiece.

### Hello protocol

- **Arguments:** -
- **Returns:** -

The Hello protocol allows a node to notify all nodes in the network that it just joined the network. This is particularly useful if said node exited the chord without notifying other nodes and without moving its keys to its successor; the network can still answer queries because several replicas are probably stored in different nodes (they might also end up in the same node that is down, although it is highly unlikely), but when the shutdown node comes back online it might have issues with not having deleted files that were deleted while it was down.

When called, the Hello protocol sends to the successor a message with format

```
HELLO <SenderId>
```

and closes the socket.

When a node receives a `HELLO` message it should:

- Close the incoming socket.
- Open a new socket to its successor.
- Forward the message to its successor.
- Close the socket to the successor.

When a node receives a `HELLO` message with its own `<SenderId>` it ignores the message, as it means the message has gone all the way around the system.

### Leave protocol

The Leave protocol allows a node to leave the system, and is quite similar to the Join protocol.

Let $s$ be the successor of the leaving node $r$.

On leave, the leaving node $r$ must perform two steps:

1. Update the predecessors and fingers tables of other nodes
2. Transfer objects from itself to its successor

#### Update predecessors and fingers tables of other nodes

Node $r$ can achieve this by using first the SetPredecessor protocol, with its predecessor's key and socket address.

To update other nodes' fingers tables, $r$ uses the FingersRemove protocol.

#### Transfer objects from itself to its successor

By now the network has no trace of $r$, except that some datapieces are missing from $s$. To fix that, $r$ does the following for each datapiece it is storing:

1. Load that datapiece into memory
2. Call the Delete protocol for that datapiece
3. Send a `PUT` message to $s$, with the `<NodeKey>` being $s$ and the key and body corresponding to the datapiece that is being moved.

### Reclaim protocol

- **Arguments:** New storage space
- **Returns:** -

When a node calls the Reclaim protocol, it says it wants to change the memory size it allows the program to use.

If the new storage space is larger than the current storage space being used, the storage space is only increased and nothing more happens.

If the new storage space is smaller than the current storage space being used, then some datapieces will have to be relocated.

To do that, the node $r$ does the following:

1. Change the maximum allowed storage space to match the new storage space
2. While the current storage space being used is larger than the maximum allowed storage space
   1. Pick one random datapiece $r$ is storing locally; assume the datapiece has key $k$
   2. Load the datapiece contents to memory
   3. Run the DeleteSystem protocol for key $k$
   4. Run the PutSystem protocol for key $k$ and the corresponding datapiece data

If any of the following operations fails, the protocol fails but it is absolutely guaranteed that the node that called Reclaim will meet its new maximum allowed storage space, even if a datapiece is deleted permanently.
