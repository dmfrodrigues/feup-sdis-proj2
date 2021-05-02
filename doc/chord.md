## Chord module

This module provides very basic interfaces to use the chord protocol. It allows one to:

- Ask for the socket address and key of the successor of a key
- Ask for the predecessor of a node for which we know the socket address
- Set the predecessor of the current node to a new value
- Inform all nodes that need to know that a new node was added to system and they should update their fingers tables
- Inform all nodes that need to know that a node was removed from the system and they should update their fingers tables

### GetSuccessor protocol

- **Arguments:** key
- **Returns:** key and socket address of the successor of that key

The GetSuccessor protocol allows to find the successor of a certain key $k$.

Let $d = distance(r, s)$ be the distance from $r$ to $k$; $r' = r + 2^{\lfloor\log_2{d}\rfloor}$ is the key with the largest power of two that is less than or equal to $d$. It is trivial that $r' ≤ r \implies successor(r') ≤ successor(r)$; as such, by going to $successor(r')$ we are also getting closer to $successor(k)$.

Thus, we use the fingers table of $r$, and jump to $successor(r')$, which is in the fingers table at index $i=\lfloor \log_2{d} \rfloor$; the base case is when $predecessor(r) < k ≤ r$, in which case node $r$ is responsible for key $k$.

When the GetSuccessor protocol is invoked in a node $r$ to find the successor of $k$, the node:

1. Checks if it is the successor of $k$, by checking if $predecessor(r) < k ≤ r$
   1. If so, $r$ returns its key and socket address
2. Determines the distance $d = distance(r, s)$ and consults its fingers table at index $i = \lfloor \log_2{d} \rfloor$, storing $r' = r.fingers[i]$
3. Sends $r'$ a message with format
```
GETSUCCESSOR <UUID>
```
instructing $r'$ to reply with the key and socket address of the successor of `<UUID>` (which is $k$).

On receiving a `GETSUCCESSOR` message, $r'$ triggers the GetSuccessor protocol for itself. The response should be in format `<SuccessorId> <IP>:<Port>`, containing the key and socket address of the successor of key $k$.

If a new node joins the network, it can simply send a `GETSUCCESSOR` to its gateway node without starting the GetSuccessor protocol locally; it would serve no purpose for the joining node to run the GetSuccessor protocol, as it has not yet built its fingers table.

### GetPredecessor protocol

- **Arguments:** node we are asking to
- **Returns:** predecessor of said node

Any node $r$ can ask about the predecessor of any other node $s$, as long as it has the socket address of $s$. To do that, $r$ sends a message to $s$ with format

```
GETPREDECESSOR
```

to which $s$ answers in format `<Key> <IP>:<Port>`, containing the key and socket address of the predecessor of $s$.

### UpdatePredecessor protocol

- **Arguments:** key, node socket
- **Returns:** -

Every node $r$ with a fingers table can run the UpdatePredecessor protocol. It sends a

```
UPDATEPREDECESSOR <SenderId> <IP>:<Port>
```

message to the current node's successor, containing a key and socket address, and the node receiving this message has to update its predecessor to correspond to the received key and socket address.

### FingersAdd protocol

- **Arguments:** -
- **Returns:** -

The joining node $r$ must deduce which nodes need to update their fingers tables. To do that, it will find each predecessor of $r - 2^i$ for $0 ≤ i < m$, and send to each of those nodes a message with format

```
FINGERADD <key> <IP>:<port> <fingerIdx>
```

which instructs the node $s$ that receives this message to check if $r$ is the new $i$-finger of $s$. The node $s$ checks if $r$ is its new $i$-finger by testing if $distance(s, r) < distance(s, s.finger[i]$; if it's false, just ignore; if it's true, it updates $s.finger[i]$ and all indices before $i$ if necessary, and forwards the `FINGERADD` message to its predecessor without changing it.

### FingersRemove protocol

- **Arguments:** -
- **Returns:** -

The leaving node $r$ must deduce which nodes need to update their fingers tables. To do that, it will find each predecessor of $r - 2^i$ for $0 ≤ i < m$, and send to each of those nodes a message with format

```
FINGERREMOVE <oldKey> <oldIP>:<oldPort> <newKey> <newIP>:<newPort> <fingerIdx>
```

where `<oldKey>` and `<oldIP>:<oldPort>` are the key and socket address of $r$, `<oldKey>` and `<oldIP>:<oldPort>` are the key and socket address of the successor of $r$ which is $r'$, and `<fingerIdx>` is $i$.

This message instructs the node $s$ that receives this message to check if $r$ is the old $i$-finger of $s$. The node $s$ checks if $r$ is its old $i$-finger by testing if $s.finger[i] = r$; if it's false, just ignore; if it's true, it updates $s.finger[i]$ and all indices before $i$ if necessary to becode $r'$, and forwards the `FINGERREMOVE` message to its predecessor without changing it.
