## Chord module

### GetSuccessor protocol

- **Arguments:** another peer, UUID
- **Returns:** UUID successor's socket address and key

The GetSuccessor protocol allows to find the successor of a certain ID.

To locate the successor of $i$ starting in node $r$, assume that all nodes with key less than $r$ are "after" $r$, after we add $2^m$ to those nodes' keys. That makes it easier to picture things.

let $r'=2^{\lfloor\log_2{i-r}\rfloor}$ be the largest power of two that is less than or equal to $i-r$. It is trivial that $r' ≤ r \implies successor(r') ≤ successor(r)$; as such, by going to $r'$ we are also getting closer to $r$.

Thus, we use the fingers table, and jump to the successor of $r'$, which is in the fingers table in the line where $k=\lfloor \log_2{i-r} \rfloor$; the base case is when $predecessor(r) < r ≤ r$, in which case node $r$ is responsible for key $i$.

When the GetSuccessor protocol is invoked in a peer $r$ to find the successor of $i$, the peer:
1. Checks if it is the successor of $i$, by checking if $predecessor(r) < i ≤ r$
   1. If so, return its IP address
2. Consults its fingers table at index $k = \lfloor \log_2{i-r} \rfloor$, storing $r' = r.fingers[k]$
3. Sends $r'$ a message with format

```
GETSUCCESSOR <UUID>
```

instructing $r'$ to reply with the socket address of the successor of `<UUID>` (which is $i$).

On receiving a `GETSUCCESSOR` message, $r'$ triggers the GetSuccessor protocol for itself. The response should be in format `<SuccessorId> <IP>:<Port>`.

### UpdatePredecessor protocol

- **Arguments:** current peer, current peer's successor
- **Returns:** -

The only peer that needs to update its predecessor is the successor of the fresh peer. The fresh peer sends a

```
UPPREDECESSOR <SenderId> <IP>:<Port>
```

message to its successor, and the peer receiving this message has to update its predecessor to correspond to the peer that sent the message.

### UpdateFingers protocol

- **Arguments:** current peer
- **Returns:** -

The fresh peer must deduce which peers need to update their fingers tables. To do that, it will find each predecessor of $r - 2^i$ for $0 ≤ i < m$, and run the UpdateFingers protocol for each of those peers.

This protocol uses the UPDATEFINGER message with format:

```
UPDATEFINGER <s> <i>
```

which instructs the peer $n$ that receives this message to check if $s$ is the new $i$-finger of that peer. A peer checks if $s$ is its new $i$th finger by testing if $n < s < finger[i]$; if it's false, just ignore; if it's true, it updates $finger[i]$ and all indices before $i$ if necessary, and calls the same protocol for its predecessor.
