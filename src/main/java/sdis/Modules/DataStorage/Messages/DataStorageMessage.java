package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.Peer;

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
        return sendTo(new Socket(address.getAddress(), address.getPort()));
    }

    public T sendTo(Socket socket) throws IOException, InterruptedException {
        socket.getOutputStream().write(this.asByteArray());
        return parseResponse(readAllBytesAndClose(socket));
    }
}
