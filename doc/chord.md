# Chord module

The *successor* of a key $k$ ($successor(k)$) is the peer with the least key that is larger than or equal to $k$.

The *predecessor* of a key $k$ ($predecessor(k)$) is the peer with the greatest key that is smaller than $k$.

A node $r$ is responsible for all keys for which their successor is $r$.

Let the key be a binary unsigned number with $m$ bits. This means there are $2^m$ different keys, from $0$ to $2^M-1$ inclusive. Let $M = 2^m$ be the modulus we are operating with (i.e., given a key $k$, because we are working with a circumference with perimeter $M$ we can say $k \equiv k + i \cdot M, \forall i \in \mathbb{Z}$).

The *distance* between nodes $a$ and $b$ is defined as the number of increments to $a$ that we need to arrive to $b$ in the modulus-$M$ space. That is, $distance(a, b) = (b-a+M) \mod M$.

## GetSuccessor protocol

- **Arguments:** UUID
- **Returns:** UUID successor's socket address and key

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

## GetPredecessor protocol

- **Arguments:** node we are asking to
- **Returns:** predecessor of said node

Any node $r$ can ask about the predecessor of any other node $s$, as long as it has the socket address of $s$. To do that, $r$ sends a message to $s$ with format

```
GETPREDECESSOR
```

to which $s$ answers in format `<Key> <IP>:<Port>`, containing the key and socket address of the predecessor of $s$.

## UpdatePredecessor protocol

- **Arguments:** -
- **Returns:** -

While $r$ is joining the system, the only node that needs to update its predecessor is the successor of the joining node $r$. After building its fingers table, $r$ sends an

```
UPDATEPREDECESSOR <SenderId> <IP>:<Port>
```

message to its successor, containing $r$ and its socket address, and the node receiving this message has to update its predecessor to correspond to $r$.

## UpdateFingers protocol

- **Arguments:** -
- **Returns:** -

The joining node $r$ must deduce which nodes need to update their fingers tables. To do that, it will find each predecessor of $r - 2^i$ for $0 ≤ i < m$, and send to each of those nodes a message with format

```
UPDATEFINGER <key> <IP>:<port> <fingerIdx>
```

which instructs the node $s$ that receives this message to check if $r$ is the new $i$-finger of $s$. The node $s$ checks if $r$ is its new $i$-finger by testing if $distance(s, r) < distance(s, s.finger[i]$; if it's false, just ignore; if it's true, it updates $s.finger[i]$ and all indices before $i$ if necessary, and forwards the `UPDATEFINGER` message to its predecessor without changing it.
