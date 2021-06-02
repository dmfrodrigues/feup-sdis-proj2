# JSSE

We implemented two versions, one using the SSLSocket and another with the SSLEngine. Both versions are slower than the non-encrypted version of the project, as expected.

## SSLSocket

Coming from a `Socket`-based implementation, using SSLSocket is straightforward: we passed the certificate JVM parameters via the system properties and started the handshake before a connection, closing it when it's done.

## SSLEngine

For the SSLEngine implementation, we created two classes, `SecureSocketChannel` (extending `SocketChannel`) and `SecureServerSocketChannel` (extending `ServerSocketChannel`), that handles the negotiation process, exchanging the protocol parameters, writing/reading from a socket channel and correctly closing the sockets.

In a `Socket`-based implementation, because each side of the TCP connection was only used to send information once, the end of a message could simply be signalled by closing that end of the TCP connection. But with TLS it is more arduous to make it work correctly, so we used a special flag byte (`FLAG`, `0x7E`) to signal the end of a message. This flag would be placed in the app data buffer after the user-provided information. This flag is then decoded at the receiving end and used by the `doRead` function to exit the `doRead` cycle.

Because there is a risk (even if minute) that the message body contains a flag byte, we used a byte stuffing mechanism to unambiguate that situation; in this strategy, an escape byte (`ESC`, `0x7D`) is defined and all data bytes $b$ equal to `FLAG` or `ESC` are encoded into two bytes, the first being `ESC` and the second being $b' = b \xor \texttt{0x20}$; thus, during the unstuffing process, if `ESC` is found, the next character is decoded with $b = b' \xor \texttt{0x20}$, as the relation $x = (x \xor y) \xor y$ is trivial.

### Issues with SSLEngine

Although `SSLEngine` is frequently referred as being *flexible*, it is so flexible that it is nearly impossible to use. The documentation relative to `SSLEngine` is very scarce, examples are short, incomplete and rather useless. Besides, the workings of the `SSLEngine` are impenetrable.

The fact that using `SSLEngine` is a requirement to reach the maximum project grade is rather concerning, as it does not present much of an improvement over `SSLSocket` in this project. Also, it requires a very large amount of time to study, implement and debug when compared to the relatively small payoff of 1/20 in the project grade, given it is a 4-people project implemented over about 7 weeks during a semester with four more curricular units. It is thus hard to understand why this is a requirement to reach maximum grade, unless the goal is to limit the number of people that reach that goal, or simply deter the students from paying any interest to this project.

We nevertheless managed to implement this feature.
