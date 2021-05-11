## System storage module

Supports the same operations as the data storage module (add, retrieve and delete datapieces) except when called the PutSystem, GetSystem and DeleteSystem protocols do not require to know where the datapieces should be stored according to their keys, and these protocols can be called from any node (while data storage protocols could only be called from the node it was supposed to apply).

<!--
DeleteSystem also handles the case where a peer that is essential to running the Delete protocol is down, using the Hello protocol to realise a node is online and retry to delete the datapiece.
-->

It additionally supports protocols to:

- Join the system in an orderly fashion
- Change maximum allowed storage space for the current node
- Leave the system in an orderly fashion

One assumption we make is that the vast majority of datapieces stored using the DataStorage layer are stored in the node they are meant to, and not in their successors due to widespread memory shortage. It is a pretty big assumption, however it 

### PutSystem protocol

- **Arguments:** UUID, data
- **Returns:** -

Finds the successor $s$ of the key of the given UUID, sends a `PUT` message to $s$ and returns the response to that message.

### DeleteSystem protocol

- **Arguments:** UUID
- **Returns:** -

Finds the successor $s$ of the key of the given UUID, sends a `DELETE` message to $s$ and returns the response to that message.

### GetSystem protocol

- **Arguments:** UUID
- **Returns:** data

Finds the successor $s$ of the key of the given UUID, sends a `GET` message to $s$ and returns the response to that message.

### MoveKeys protocol

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

TODO/TO-THINK:

As for the datapieces that $s$ points to its successor, $r$ only has to point to its successor;

As for the datapieces whose base is in $s$ and should be in $r$, we delete them and re-store them.

### RemoveKeys protocol

By now the network has no trace of $r$, except that some datapieces are missing from $s$. To fix that, $r$ uses the RemoveKeys protocol to move the datapieces it still has to somewhere else in the network, by doing the following for each datapiece it is storing:

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
