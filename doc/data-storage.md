## Data storage module

This is a very simple data storage protocol, which is tasked with storing, retrieving and deleting datapieces in a specific node, taking into account storage limits of each node. Does not guarantee that a datapiece is accessible if the node storing it is not shutdown in an orderly fashion.

### Put protocol

- **Arguments:** a node key, datafile key, data
- **Returns:** -

When the Put protocol is called without a node key, it defaults to the own node's key (because this node key corresponds to the original caller of the Put protocol; if the request wraps around the whole system and comes back to the node that initially called the Put protocol, the node must signal the error); when the Put protocol is called, said node has to save the datapiece using the specified key, using its successors if necessary.

Upon starting this protocol, a node $r$ should perform the following actions:

1. If the original key is equal to the successor of $r$, then it fails.
2. If $r$ has not stored that datapiece and does not have space for another piece:
     1. It registers locally that the datapiece is being stored in its successor.
     2. It sends to its successor $s = successor(r)$ a message with format
```
PUT <NodeKey> <DataKey><LF><Body>
```
where NodeKey is the node key the protocol was called with
     4. If $s$'s answer fails, $r$ removes its local registry saying that the datapiece was stored in its successor.
     5. $r$ returns according to the answer it got from $s$.
3. If $r$ has already stored that datapiece:
     1. It returns with success.
4. If $r$ has not stored that chunk but has a pointer to its successor reporting that it might have stored:
     1. If it has space for that chunk:
          1. It stores the chunk locally.
          2. It asynchronously sends to its successor $s = successor(r)$ a `DELETE` message to delete that chunk.
          3. It returns successfully
     2. If it does not have space for that chunk:
          1. It sends a `PUT` message to its successor $s = successor(r)$.
          2. It replies to the original message with whatever $s$ replied.
5. If $r$ has not yet stored that chunk and has available space:
     1. It stores that chunk;
     2. It returns with success.

Upon receiving a `PUT` message, the node should start the Put protocol locally using the node key it got from the `PUT` message (not its own key), and answer the `PUT` message according to what the Put protocol returns.

### Delete protocol

- **Arguments:** key
- **Returns:** -

When the Delete protocol is called for a certain node, said node assumes it has that datapiece, and tries to delete it.

Upon starting this protocol, a node $r$ should perform the following actions:

1. If $r$ has not stored that datapiece and has no pointer saying its successor stored it,
     1. It replies with success.
2. If $r$ has stored that datapiece:
     1. It deletes the datapiece.
     2. It replies with success.
3. If $r$ has not stored that datapiece but has a pointer to its successor reporting that it might have stored:
     1. It sends to its successor $s = successor(r)$ a message with format
```
DELETE <Key>
```
with the key of the file it intends to delete.

     2. It replies to the original message according to what $s$ replied.

Upon receiving a `DELETE` message with a certain key, a node starts the Delete protocol for itself in order to delete the datapiece with the mentioned key.

TODO:

We also have to take into account the scenario where a node is down; say a node is down and it was storing a certain replica, and afterwards it is requested for that chunk to be deleted:

- If the node ran the chord leaving protocol, then on shutdown it contains no replicas at all, and all the replicas it had were transferred to another node; in this case we do not have to worry, as all replicas of a chunk are active in the system at all times, and upon running the Delete protocol it is guaranteed that all replicas will be deleted.
- If the node did not run the chord leaving protocol, then the node that failed to contact another node and ask it to be deleted must remember that, so that it can retry to delete when the node rejoins the system.

### Get protocol

- **Arguments:** key
- **Returns:** data

The Get protocol allows a node to get a certain datapiece by its key, assuming it has the datapiece or that one of its successors has it.

Upon calling the Get protocol locally, node $r$ does the following:

1. If it has stored the datapiece
     1. It returns the datapiece
2. If it has not stored the datapiece but knows its successor has
     1. It sends a message to its successor $s = successor(r)$ with format
```
GET <UUID>
```

     2. It returns according to the response from $s$.
3. If it has not stored the datapiece nor points to its successor for further information
     1. It fails

Upon receiving a `GET` message, a node starts the Get protocol locally, and responds to the message according to whatever the Get protocol returns.


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
