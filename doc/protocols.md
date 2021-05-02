# Protocols

We have devised a system with a significant amount of different protocols; this was so we could reuse as much code as possible, and had the additional advantage that each protocol is quire easy to implement and easy to reason on, as each protocol aims at performing a very concrete (and often simple) task, and it is easy to understand which protocols will use which protocols. It has the inconvenience that the diagram depicting relations between protocols is quite dense.

The peer is allowed to directly use the following protocols:

\vspace{-0.1em}
\begin{multicols}{3}
    \begin{itemize}
        \itemsep0em
        \item Authenticate
        \item Join
        \item BackupFile
        \item RestoreFile
        \item DeleteFile
        \item Leave
    \end{itemize}
\end{multicols}

![Protocols diagram](protocols.svg)

\newpage

## Definitions

The *successor* of a key $k$ ($successor(k)$) is the peer with the least key that is larger than or equal to $k$.

The *predecessor* of a key $k$ ($predecessor(k)$) is the peer with the greatest key that is smaller than $k$.

A node $r$ is responsible for all keys for which their successor is $r$.

Let the key be a binary unsigned number with $m$ bits. This means there are $2^m$ different keys, from $0$ to $2^M-1$ inclusive. Let $M = 2^m$ be the modulus we are operating with (i.e., given a key $k$, because we are working with a circumference with perimeter $M$ we can say $k \equiv k + i \cdot M, \forall i \in \mathbb{Z}$).

The *distance* between nodes $a$ and $b$ is defined as the number of increments to $a$ that we need to arrive to $b$ in the modulus-$M$ space. That is, $distance(a, b) = (b-a+M) \mod M$.
