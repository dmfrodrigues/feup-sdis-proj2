## Chord module

This module provides very basic interfaces to use the chord protocol. It allows one to:

- Ask for the socket address and key of the successor of a key
- Ask for the predecessor of a node for which we know the socket address
- Set the predecessor of the current node to a new value
- Inform all nodes that need to know that a new node was added to system and they should update their fingers tables
- Inform all nodes that need to know that a node was removed from the system and they should update their fingers tables

Our implementation closely follows the one provided in [@stoica2001].

Consider that $x \in [a, b]$ means that $distance(a, x) \leq distance(a, b)$.

### Simple messages

#### `SUCCESSOR` message

| **Request** | | **Response**            |
|-------------|-|-------------------------|
| `SUCCESSOR` | | `<NodeKey> <IP>:<Port>` |

Upon receiving this message, the node responds with its locally-stored successor, which is its 0-th finger.

#### `PREDECESSOR` message

| **Request**   | | **Response**            |
|---------------|-|-------------------------|
| `PREDECESSOR` | | `<NodeKey> <IP>:<Port>` |

Upon receiving this message, the node responds with its locally-stored predecessor.

### FindPredecessor protocol

- **Arguments:** node we are asking to
- **Returns:** predecessor of said node

This protocol allows to find the predecessor of a certain key $k$.

We know a node $n'$ (with successor $s'$) is the predecessor of $k$ iff $k \in (n', s']$. Thus, until we do not reach that condition, we must update $n'$ for it to get ever closer to meeting that condition. We can do that by successively updating $n'$ with the approximation $n'$ gives for the predecessor of $k$, through the `CPFINGER` message.

#### `CPFINGER` message

| **Request**      | | **Response**            |
|------------------|-|-------------------------|
| `CPFINGER <Key>` | | `<NodeKey> <IP>:<Port>` |

Instructs $n'$ to search, using its fingers table, for the largest finger $f$ that precedes $k$.

### FindSuccessor protocol

- **Arguments:** key
- **Returns:** key and socket address of the successor of that key

The FindSuccessor protocol allows to find the successor of a certain key $k$. It essentially performs the heavy-lifting using the FindPredecessor protocol, and then sends that node a `SUCCESSOR` message.

### SetPredecessor protocol

- **Arguments:** node key and socket
- **Returns:** -

This protocol can be called to set the predecessor of the successor $s$ of a node $r$ to a specific node. It consists of sending a `SETPREDECESSOR` message to the successor $s$ of node $r$.

#### `SETPREDECESSOR` message

| **Request**                            | | **Response** |
|----------------------------------------|-|--------------|
| `SETPREDECESSOR <NodeKey> <IP>:<Port>` | | None         |

Instructs a node $s$ to set its predecessor to the values on the message.
This message requires no response, only that the receiving end closes the connection.

### FingersAdd protocol

- **Arguments:** -
- **Returns:** -

The joining node $r$ must deduce which nodes need to update their fingers tables. To do that, it will find each predecessor of $r - 2^i$ for $0 ≤ i < m$, and notify each of those nodes to the possibility that it might have to change its $i$-th finger given that $r$ is a possible candidate for that finger, using `FINGERADD` messages.

#### `FINGERADD` message

| **Request**                                   | | **Response** |
|-----------------------------------------------|-|--------------|
| `FINGERADD <NodeKey> <IP>:<Port> <FingerIdx>` | | None         |

Instructs a node $r$ receiving this message that it should check if the node $f$ referenced in the message is its new $i$-th finger.
This message requires no answer, only that the socket is closed once the request is fulfilled.

Upon receiving this message, $r$ first checks if the message is instructing it to check if it is itself its $i$-th finger;
because this protocol is only called on joining, and in this case the sending node has already built its fingers table correctly it just ignores the message.

We otherwise arrive at the non-trivial stage, where $r$ checks if $f$ is its new $i$-finger by testing if $distance(r + 2^i, f) < distance(r + 2^i, k)$;
if it's false, just ignore;
if it's true:

1. Update $r.finger[i]$ and all indices before $i$ if they also comply to that condition
2. Forward the `FINGERADD` message to its predecessor $p$ without changing it.

One can also check if $p \neq f$ before performing step 2, as it already knows the message will be ignored by $p$ due to the initial check; however, this check avoids an additional TCP connection, thus we do both checks.

A node receiving a `FINGERADD` message currently only closes the incoming socket after having performed all processing, including forwarding the `FINGERADD` message and waiting for that request to be over; this is opposite to the most desirable option, where a node receiving a `FINGERADD` message would immediately close the incoming socket so as not to exhaust the number of TCP connections.

We decided to go with the first option, as it is better for testing, since the FingersAdd protocol will only end once all nodes that had to update their fingers tables are done; otherwise, it is relatively common for tests to begin running before the network stabilizes, and test results are unreliable unless the tests are performed after a sleep period (of about 100ms).

### FingersRemove protocol

- **Arguments:** -
- **Returns:** -

This protocol is meant to notify the network that a node $r$ is about to leave, and as such other nodes must update their fingers tables to remove any evidence that $r$ ever existed.

The operation of this protocol is the same as FingersAdd, except it sends `FINGERREMOVE` messages.

#### `FINGERREMOVE` message

| **Request**                                                                      | | **Response** |
|----------------------------------------------------------------------------------|-|--------------|
| `FINGERREMOVE <OldKey> <OldIP>:<OldPort> <NewKey> <NewIP>:<NewPort> <FingerIdx>` | | None         |

Instructs a node $r$ that, if it has a finger corresponding to the old node $f_{old}$ reported in the `FINGERREMOVE` message, that finger should be replaced by the new node $f_{new}$, as $f_{old}$ is being removed from the system.

A `FINGERREMOVE` message is processed in a very similar way to a `FINGERADD` message, starting with a simple check if $f_{old}$ is equal to the current node.

In the non-trivial stage, all that changes is that, instead of the condition being related to distances, it merely checks if fingers with a certain index $i$ or less have the same value as $f_{old}$, and if so they are replaced by $f_{new}$.

If at least one finger was changed, similarly to `FINGERADD`, the same `FINGERREMOVE` message is forwarded to the current node's predecessor, except if said predecessor is equal to $f_{old}$, in which case this step is ignored.


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

##### Get predecessor

$r$ gets its successor by asking $g$ for its 0-th finger. Then it sends a `PREDECESSOR` message to its newly-found successor. Because the successor of $r$ was not yet told that $r$ joined the network, it will return what it thinks is its predecessor, when actually it is the predecessor of $r$.

##### Build fingers table

To build its fingers table, $r$ asks to $g$ what is the successor of $k = r + 2^i$, for all values of $0 ≤ i < m$, using several `FINDSUCCESSOR` messages directed at $g$.

The joining node can asynchronously make all `FINDSUCCESSOR` requests to $g$, but that yields $O(m \log N)$ time complexity, while [@stoica2001] states that, if requests are made sequentially, some trivial tests can be made to skip asking about some fingers. We decided to keep everything asynchronous and ignore this improvement.

#### Update other nodes

The joining node $r$ needs to notify other nodes in the system to update the information they have about the system.
Node $r$ must namely notify its successor that $r$ is its new predecessor, using protocol SetPredecessor.
It must also notify some of the other nodes in the system to update their fingers tables with its own key and socket address, by running one instance of the FingersAdd protocol.

#### Move keys

To move the keys (and perform any other operations the upper layer deems necessary), the Join protocol accepts a runnable, which the upper layer must provide, specifying how to move keys between nodes.

The upper layers are likely to use an instance of the GetRedirects protocol, and another of the MoveKeys protocol.

### Leave protocol

The Leave protocol allows a node to leave the system, and is quite similar to the Join protocol.

Let $s$ be the successor of the leaving node $r$.

On leave, the leaving node $r$ must perform two steps:

1. Update the predecessors and fingers tables of other nodes
2. Transfer objects from itself to its successor

#### Update predecessors and fingers tables of other nodes

Node $r$ can achieve this by using first the SetPredecessor protocol, with its predecessor's key and socket address.

To update other nodes' fingers tables, $r$ uses the FingersRemove protocol.

#### Move keys again

The upper layer must provide a runnable to execute whatever actions are required to move the keys from the leaving node to the rest of the system.

The upper layer is likely to use an instance of the RemoveKeys protocol.
