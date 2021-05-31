# Fault-tolerance

We implemented provisions for two types of peer shutdown.

## Gracefully leaving

A peer is said to *gracefully leave* the system if it is allowed to perform some actions before leaving the system. This consists of informing some nodes of the system that it is about to leave (and as such that they must update their predecessor/fingers), as well as transfer its keys and files to its successor. This is the expected behavior of a peer that is being shutdown normally for maintenance, if the network is being reorganized or if there is a power outage and the peer has a few minutes before backup batteries run out of energy.

This case is quite trivial to solve. The leaving peer $u$ must do what we just mentioned, consisting of:

1. Informing the successor of $u$ that its precedessor is no longer $u$, but $u$'s predecessor.
2. Informing some nodes that they should change their fingers tables if they have $u$ as one of their fingers.
3. For each file it is storing, delete it locally and re-store it in the system (since all other peers' tables have already been updated, the system no longer knows that $u$ exists, and will store the datapieces somewhere else, ignoring $u$ in the process)

Finally, $u$ cleans up any remains of its activity from disk and exits.

## Abnormal leaving

A peer is said to *abnormally leave* the system if it does not have time to perform some or all actions it needs for the leaving to be considered graceful. This is the expected behavior of a peer that finds an exception, or is suddenly shutdown before leaving or even if it was in the middle of leaving. This is a problem because the information in that peer was not transferred to the rest of the system nor is there a guarantee as for when the peer will rejoin the system, or if it will rejoin at all.

To solve this problem, we first assume that, once a peer leaves the system, the data it was storing is definitely lost. This allows a simpler analysis of the problem. As such, if a peer is rejoining the system, it must first delete any remnants of previous runs of that peer in the current system. Also, if a peer that the system thought existed fails to respond to a request, the system assumes the peer has unexpectedly shutdown and assumes all data it had stored as permanently lost.

This means the `SystemStorage` module does not guarantee data is not lost and might fail to retrieve some datapieces that were successfully stored before. This problem is solved by the upper layer (`Main`), which replicates the same chunk several times, and as such can retrieve another replica of the same chunk, and latter re-store the replica it failed to find.

### Periodic consistency checks

<!-- TODO -->

We also implemented periodic consistency checking procedures, which are scheduled to run every $\SI{10}{\second}$ and allow the system to check it is consistent. Consistency must be achieved at two levels:

- **Peer referencing** | The `Chord` module must correctly know its predecessor and all its fingers, and that they are all online.
- **Data** | The `Main` module must assure all replicas of all chunks of all files are accessible.

#### Chord consistency

We run the chord consistency checks every $\SI{10}{\second}$.

##### Assuring successor correctness

No mather what, each node must aggressively maintain an ordered list of $N$ successors, in case a few of those successors fail without notice. The ClosestPrecedingFinger algorithm tries to return the largest finger that precedes key $k$, but if it fails to find that finger it must go on to lower-index fingers until it reaches finger 0; if even so the finger corresponds to a node that does not exist, the algorithm must go on to explore the ordered list of $N$ successors and pick the first valid successor, so it can keep making progress. Thus, if we assure the successors list is somewhat correct we can also assume that FindPredecessor and FindSuccessor are working correctly.

To assure this, each node keeps a list $L$ of at most $N = 10$ successors. If the number $V$ of nodes in the chord is less than $N$ the list only has $V$ elements, and if $V > N$ the list has $N$ successors. A node $n$ periodically corrects $L$ by finding, in order, the first element of $L$ that is valid; then it considers that node to be the successor and the first element of the list $s_0$, and asks that node about its successor $s_1$ using a `SUCCESSOR` message; then $n$ asks $s_1$ about its successor $s_2$, and so on, until it knows the information about $s_{N-1}$.

##### Assuring fingers correctness

Since we already assured FindSuccessor to be working, we can also recalculate our fingers periodically.

##### Assuring predecessor correctness

Predecessor correctness can be trivially assured by calling the FindPredecessor protocol for the current node.

#### Data consistency

We run the data consistency checks every $\SI{1}{\minute}$.
