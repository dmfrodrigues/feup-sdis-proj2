# Scalability

## Design level

We fully implemented the Chord protocol (`Chord` module), with keys having $\SI{30}{\bit}$, which we chose as it fits in a $\SI{32}{\bit}$ signed integer, and does not use the corner cases where numbers are close to the limits the types can represent.

## Implementation level

As we described in-depth in section \ref{sec:concurrency-design}, we are using Java NIO to read/write files, a thread pool to process incoming messages and a `ForkJoinPool` to manage protocols on the initiator and receiver sides.
