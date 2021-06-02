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

To check if another peer is operating, the current peer simply opens a socket connecting to that peer; this will fail if the peer does not accept the connection, as we are speaking of TCP connections which require a handshake. Ideally, this socket will then be supplied to the protocol that needs it, but if the only goal is to check the peer exists and get its information (without needing a socket), a `HELLO` message will be sent, with the only goal of telling the receiving end to respond with a `1` and ignore (otherwise the receiving end could get confused as to the type of the message, as it would otherwise be empty).

### At the chord level

#### How to achieve chord correctness

##### Assuring successor correctness

No mather what, each node must aggressively maintain an ordered list of $N$ successors, in case a few of those successors fail without notice. The ClosestPrecedingFinger algorithm tries to return the largest finger that precedes key $k$, but if it fails to find that finger it must go on to lower-index fingers until it reaches finger 0; if even so the finger corresponds to a node that does not exist, the algorithm must go on to explore the ordered list of $N$ successors and pick the first valid successor, so it can keep making progress. Thus, if we assure the successors list is somewhat correct we can also assume that FindPredecessor and FindSuccessor are working correctly.

To assure this, each node keeps a list $L$ of at most $N = 10$ successors. If the number $V$ of nodes in the chord is less than $N$ the list only has $V$ elements, and if $V > N$ the list has $N$ successors. A node $n$ periodically corrects $L$ by finding, in order, the first element of $L$ that is valid; then it considers that node to be the successor and the first element of the list $s_0$, and asks that node about its successor $s_1$ using a `SUCCESSOR` message; then $n$ asks $s_1$ about its successor $s_2$, and so on, until it knows the information about $s_{N-1}$.

When joining the system, after the joining peer $n$ performs all usual steps, it will send `NTFYSUCCESSOR` (*notify successor*) messages to its at most $N$ predecessors, telling them that $n$ joined the system and that they might want to consider the possibility of adding $n$ to their successors lists. When leaving the system in an orderly fashion, the leaving peer sends `UNTFYSUCCESSOR` (*unnotify successor*) messages to its at most $N$ predecessors, telling them to remove $n$ from their successors lists if $n$ is in those lists; currently this message triggers a full re-computation of the successors list.

##### Assuring fingers correctness

Since we already assured FindSuccessor to be working, we can also recalculate our fingers when needed.

##### Assuring predecessor correctness

Predecessor correctness can be trivially assured by calling the FindPredecessor protocol for the current node.

#### Corrections

We implemented mid-operation corrections to allow corruptions to be fixed while performing a certain action. For instance, say a peer needs to connect to a finger, but that finger is offline; when the peer opens the connection with another peer it notices the other peer did not accept the connection. Instead of aborting the protocol and having to wait for periodic consistency checks, the peer launches an instance of the FindSuccessor protocol to fix that finger, and only then does it return that finger.

#### Periodic consistency checks: FixChord protocol

We run the chord consistency checks every $\SI{10}{\second}$. This protocol:

1. Recalculates all $N$ successors.
2. Gets all fingers, which will trigger a finger fix for each finger that is offline.
3. Update predecessor.

### At the file level

#### Corrections

We have already described on the section about the Main module how we perform corrections of file inconsistencies (i.e., if we fail to find a replica of a certain chunk, we re-store it).

#### Periodic consistency checks: FixMain protocol

We run the data consistency checks every $\SI{1}{\minute}$. This protocol consists of:

1. Identifying all the users for which at least one replica of a chunk of their metadata files is stored locally.
2. For each user, get its user metadata file using an instance of the `RestoreFile` protocol
3. For each user, for each file the user has stored, call the FixFile protocol.

The FixFile protocol works similar to the RestoreFile protocol, but instead uses mostly HeadSystem messages to check the replicas exist; if not, it retrieves one of the reachable replicas and uses that to restore all the missing replicas of that chunk.
