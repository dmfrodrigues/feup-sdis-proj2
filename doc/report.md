# P2P Chord backup protocol <!-- omit in toc -->

- Let $h$ be the consistent hash function, where $h(s)$ is the hash of string $s$.

The *successor* of a key $i$ ($successor(i)$) is the peer with the least key that is larger than or equal to $i$.

The *predecessor* of a key $i$ ($predecessor(i)$) is the peer with the greatest key that is smaller than $i$.

A peer $r$ is responsible for all keys for which their successor is $r$.

A chunk with a certain ID $i$ and replication degree $D$ is replicated $D$ times; each replication of that chunk is called a *replica*.



## Bibliography

- Stoica, I., Morris, R., Karger, D., Kaashoek, M. F., Balakrishnan, H. (2001). Chord: A Scalable Peer-to-peer Lookup Service for Internet Applications. *SIGCOMM Computer Communication Review* *31*(4), 149-160
