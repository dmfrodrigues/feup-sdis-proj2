# Overview

This report addresses the [**second project**](https://web.fe.up.pt/~pfs/aulas/sd2021/projs/proj2/proj2.html) of course SDIS (Distributed Systems) at the Faculty of Engineering of the University of Porto (FEUP).

This project aims at designing and implementing a peer-to-peer backup system which guarantees:

- A dynamic network of peers
- Decent performance on storing/restoring/deleting files
- Fault-tolerance
- Safety

We designed an account-based service which resembles many cloud storage services from the end-user's point of view. The system can be joined by starting the PeerDriver executable:

```
java PeerDriver <ID> <RemoteAccessPoint> [<IP>:<Port>]
```

where `<IP>:<Port>` is the socket address of a gateway peer that can be used so the local peer can join the system. This argument is optional because, if the peer wants to create a new system (whether no system existed before or the peer wants to create a new, parallel system), it needs no gateway peer.

The user can interact with the peer object by calling the TestApp class, which connects to the peer running under a certain remote access point:

```
java TestApp <RemoteAccessPoint> <Username> <Password> <Operation>
```

The operation is optional (running the command does nothing, except if the account with that username did not exist, in which case it only creates the account and exits), and can be one of the following:

\begin{tabular}{@{} p{170mm} @{}}
    \hline
    \texttt{BACKUP <Origin> <Destination> <ReplicationDeg>} \\ Backup local file \texttt{<Origin>} with remote path \texttt{<Destination>} and a certain replication degree \\ \hline
    \texttt{RESTORE <Origin> <Destination>                } \\ Restore remote file \texttt{<Origin>} and store it locally to path \texttt{<Destination>}                    \\ \hline
    \texttt{DELETE <Origin>                               } \\ Delete remote file \texttt{<Origin>}                                                                         \\ \hline
    \texttt{DELETEACCOUNT                                 } \\ Delete account                                                                                               \\ \hline
    \texttt{LEAVE                                         } \\ Causes peer to leave the system in an orderly fashion; the peer process is ended as well                     \\ \hline
\end{tabular}

This means a local file can be remotely stored under any name the user wishes, allowing for great flexibility.

## Features

A basic, functioning implementation yields 12/20. To reach the 20/20 mark, we implemented the following features:

- (+2) We use thread-based concurrency as much as possible.
- (+2) We use SSLEngine in our final project version.
- (+4) Our project is scalable at the design level (chord) and implementation level (thread pools and asynchronous I/O)
- (+2) We implemented a fault-tolerant solution, with replication degrees, decentralized data storage and decentralized data indexing (user metadata files).
