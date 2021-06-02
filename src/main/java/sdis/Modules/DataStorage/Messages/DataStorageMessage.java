package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.Peer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class DataStorageMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Chord chord;
        private final DataStorage dataStorage;
        private final Socket socket;

        public Processor(Chord chord, DataStorage dataStorage, Socket socket){
            this.chord = chord;
            this.dataStorage = dataStorage;
            this.socket = socket;
        }

        public Chord getChord(){
            return chord;
        }

        public DataStorage getDataStorage(){
            return dataStorage;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract DataStorageMessage.Processor getProcessor(Peer peer, Socket socket);

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
