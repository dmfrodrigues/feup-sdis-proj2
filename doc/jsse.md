# JSSE

We implemented two versions, one using the SSLSocket and another with the SSLEngine.

## SSLSocket

The SSLSocket is straightforward, we passed the certificate JVM parameters via the system properties and started the handshake before a connection, closing it when it's done.

## SSLEngine

For the SSLEngine implementation, we created two classes, *SecureSocketChannel* and *SecureServerSocketChannel*, which extends *SocketChannel*, that treats all the negotiation process, exchanging the protocol parameters, and writing/reading from a socket channel.

Also, in order to check the end of a transmission when reading from a socket channel, we added a flag (**0x7E**) in the end of the sender data buffer.
However, because this flag could be part of the original buffer, a byte stuffing mechanism was performed. In this strategy, a **ESC** byte (**0x7D**) is stuffed before every flag byte. We then apply the reverse process to get the original data stream.
