# JSSE

We implemented two versions, one using the SSLSocket and another with the SSLEngine.

## SSLSocket

The SSLSocket is straightforward, we passed the certificate parameters and started the handshake before a connection.

## SSLEngine

For the SSLEngine implementation, we created two classes, *SecureSocketChannel* and *SecureServerSocketChannel*, that treats all the negotiation process, exchanging the protocol parameters, and reading and writing from a socket channel.

Also, in order to check the end of a transmission when reading from a socket channel, we added a flag in the end of the sender data buffer.
However, because this flag could be part of the original buffer, a byte stuffing technic was performed.
