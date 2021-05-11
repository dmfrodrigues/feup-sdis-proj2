## Data storage module

This is a very simple data storage protocol, which is tasked with storing, retrieving and deleting datapieces in a specific node, taking into account storage limits of each node. Does not guarantee that a datapiece is accessible if the node storing it is not shutdown in an orderly fashion.

### Put protocol

- **Arguments:** a node key, datafile key, data
- **Returns:** boolean indicating success

When the Put protocol is called without a node key, it defaults to the own node's key (because this node key corresponds to the original caller of the Put protocol; if the request wraps around the whole system and comes back to the node that initially called the Put protocol, the node must signal the error); when the Put protocol is called, said node has to save the datapiece using the specified key, using its successors if necessary.

Upon starting this protocol, a node $r$ should perform the following actions:

1. Find its successor $s$
2. If $r$ has stored the datapiece locally, return true
3. If $r$ has space for that datapiece
   1. Store it locally
   2. If it points to its successor, send a `DELETE` message to $s$
   3. Return successfully
4. If $s$ is the node that originally asked to put this datapiece, then return false
5. If $r$ does not yet point to $s$, make it point to $s$
6. Send a `PUT` message to $s$
7. Wait for the response, and return according to the message's response

#### `PUT` message

| **Request**                      | | **Response** |
|----------------------------------|-|--------------|
| `PUT <NodeKey> <UUID><LF><Body>` | | `<RetCode>`  |

Upon receiving a `PUT` message, the node should start the Put protocol locally using the node key it got from the `PUT` message (not its own key), and answer the `PUT` message according to what the Put protocol returns: `1` for successful, `0` otherwise.

### Delete protocol

- **Arguments:** key
- **Returns:** -

When the Delete protocol is called for a certain node, said node assumes it has that datapiece, and tries to delete it.

Upon starting this protocol, a node $r$ performs the following actions:

1. Find its successor $s$
2. If $r$ has not stored that datapiece and does not point to $s$, return true.
3. If $r$ has the datapiece stored locally
   1. Delete it locally
   2. Return true
4. Send a `DELETE` message to $s$
5. Wait for the response, and return with the appropriate value according to the response

#### `DELETE` message

| **Request**     | | **Response** |
|-----------------|-|--------------|
| `DELETE <UUID>` | | `<RetCode>`  |

Upon receiving a `DELETE` message with a certain key, a node starts the Delete protocol for itself in order to delete the datapiece with the mentioned key. It responds with the boolean value returned by the Delete protocol it started.

### Get protocol

- **Arguments:** key
- **Returns:** data

The Get protocol allows a node to get a certain datapiece by its key, assuming it has the datapiece or that one of its successors has it.

Upon calling the Get protocol locally, node $r$ does the following:

1. Find its successor $s$
2. If $r$ locally has the datapiece, return the datapiece
3. If $r$ points to its successor
     1. Send a `GET` message to $s$
     2. Returns according to the response to the `GET` message:
        1. If it fails, it returns `null`
        2. It it succeeds, it returns an array of bytes with the datapiece contents
4. It otherwise fails, and returns `null`

#### `GET` message

| **Request**  | | **Response**      |
|--------------|-|-------------------|
| `GET <UUID>` | | `<RetCode><Body>` |

Upon receiving a `GET` message, a node starts the Get protocol locally, and responds to the message according to whatever the Get protocol returns: if the protocol fails (by returning `null`), the response has a `<RetCode>` of 0 and the body is empty; if the protocol succeeds, the response has a `<RetCode>` of 1 and the body contains the contents of the datapiece.

### GetRedirects protocol

| **Request**    | | **Response**                                |
|----------------|-|---------------------------------------------|
| `GETREDIRECTS` | | `<RedirectUUID1><LF><RedirectUUID1><LF>...` |

$s$ responds to the message with the list of UUIDs of all datapieces that it is redirecting to its successor, so that $r$ can equally point to its successor.

<!--
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
-->
