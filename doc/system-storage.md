## System storage module

Supports the same operations as the data storage module (add, retrieve and delete datapieces) except when called the PutSystem, GetSystem and DeleteSystem protocols do not require to know where the datapieces should be stored according to their keys, and these protocols can be called from any node (while data storage protocols could only be called from the node it was supposed to apply).

It additionally supports protocols to:

- Join the system in an orderly fashion
- Change maximum allowed storage space for the current node
- Leave the system in an orderly fashion

The `PUTSYSTEM`, `DELETESYSTEM`, `GETSYSTEM` and `HEADSYSTEM` messages have the same formats as their corresponding Data Storage messages `PUT`, `DELETE`, `GET` and `HEAD`.

### PutSystem protocol

- **Arguments:** UUID, data
- **Returns:** -

Finds the successor $s$ of the key of the given UUID, sends a `PUTSYSTEM` message to $s$ and returns the response to that message.

### DeleteSystem protocol

- **Arguments:** UUID
- **Returns:** -

Finds the successor $s$ of the key of the given UUID, sends a `DELETESYSTEM` message to $s$ and returns the response to that message.

### GetSystem protocol

- **Arguments:** UUID
- **Returns:** data

Finds the successor $s$ of the key of the given UUID, sends a `GETSYSTEM` message to $s$ and returns the response to that message.

### HeadSystem protocol

- **Arguments:** UUID
- **Returns:** data

Finds the successor $s$ of the key of the given UUID, sends a `HEADSYSTEM` message to $s$ and returns the response to that message.

### MoveKeys protocol

- **Arguments:** -
- **Returns:** -

Requests the successor $s$ of node $r$ to re-store the keys that no longer belong to $s$ and now belong to $r$.

#### `MOVEKEYS` message

| **Request**                       | | **Response** |
|-----------------------------------|-|--------------|
| `MOVEKEYS <SenderId> <IP>:<Port>` | | None         |

$s$ does the following for each datapiece it is storing with key $k$ such that $distance(k, r) < distance(k, s)$:

1. Load the datapiece from secondary memory.
2. Start the Delete protocol for that datapiece.
3. Start the PutSystem protocol for that datapiece.

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
