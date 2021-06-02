package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Message;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class SystemStorageMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final SystemStorage systemStorage;
        private final Socket socket;

        public Processor(SystemStorage systemStorage, Socket socket){
            this.systemStorage = systemStorage;
            this.socket = socket;
        }

        public SystemStorage getSystemStorage(){
            return systemStorage;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract SystemStorageMessage.Processor getProcessor(Peer peer, Socket socket);

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
        return parseResponse(readAllBytesAndClose(socket));
    }
}
