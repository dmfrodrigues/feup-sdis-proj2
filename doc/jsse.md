# JSSE

We implemented two versions, one using the SSLSocket and another with the SSLEngine.

### SSLEngine

The

#### Byte Stuffing

In order to check the end of a transmission when reading, we added a flag in the end of the sender data buffer.
However, because this flag could be part of the original buffer, a byte stuffing technic was performed.

