package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Message;
import sdis.Peer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class MainMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Main main;
        private final Socket socket;

        public Processor(Main main, Socket socket){
            this.main = main;
            this.socket = socket;
        }

        public Main getMain(){
            return main;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract MainMessage.Processor getProcessor(Peer peer, Socket socket);

    protected abstract byte[] formatResponse(T t);

    protected abstract T parseResponse(byte[] data);

    public T sendTo(InetSocketAddress address) throws IOException, InterruptedException {
        SSLSocket socket;
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = (SSLSocket) ssf.createSocket(address.getAddress(), address.getPort());
        socket.setEnabledCipherSuites(ssf.getDefaultCipherSuites());
        socket.startHandshake();
        return sendTo(socket);
    }

    public T sendTo(Socket socket) throws IOException, InterruptedException {
        socket.getOutputStream().write(this.asByteArray());
        socket.getOutputStream().flush();
        return parseResponse(readAllBytesAndClose(socket));
    }
}
