# JSSE

We implemented two versions, one using the SSLSocket and another with the SSLEngine.

## SSLSocket

Coming from a `Socket`-based implementation, using SSLSocket is straightforward: we passed the certificate JVM parameters via the system properties and started the handshake before a connection, closing it when it's done.

## SSLEngine

For the SSLEngine implementation, we created two classes, `SecureSocketChannel` (extending `SocketChannel`) and `SecureServerSocketChannel` (extending `ServerSocketChannel`), that handles the negotiation process, exchanging the protocol parameters, writing/reading from a socket channel and correctly closing the sockets.

In a `Socket`-based implementation, because each side of the TCP connection was only used to send information once, the end of a message could simply be signalled by closing that end of the TCP connection. But with TLS it is more arduous to make it work correctly, so we used a special flag byte (`FLAG`, `0x7E`) to signal the end of a message. This flag would be placed in the app data buffer after the user-provided information. This flag is then decoded at the receiving end and used by the `doRead` function to exit the `doRead` cycle.

Because there is a risk (even if minute) that the message body contains a flag byte, we used a byte stuffing mechanism to unambiguate that situation; in this strategy, an escape byte (`ESC`, `0x7D`) is defined and all data bytes $b$ equal to `FLAG` or `ESC` are encoded into two bytes, the first being `ESC` and the second being $b' = b \xor \texttt{0x20}$; thus, during the unstuffing process, if `ESC` is found, the next character is decoded with $b = b' \xor \texttt{0x20}$, as the relation $x = (x \xor y) \xor y$ is trivial.
