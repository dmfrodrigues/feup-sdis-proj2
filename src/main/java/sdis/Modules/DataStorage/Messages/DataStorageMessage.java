package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.Peer;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class DataStorageMessage<T> extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Chord chord;
        private final DataStorage dataStorage;
        private final SocketChannel socket;

        public Processor(Chord chord, DataStorage dataStorage, SocketChannel socket){
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

        public SocketChannel getSocket(){
            return socket;
        }
    }

    public abstract DataStorageMessage.Processor getProcessor(Peer peer, SocketChannel socket);

    protected abstract ByteBuffer formatResponse(T t);

    protected abstract T parseResponse(ByteBuffer data);

    public T sendTo(DataStorage dataStorage, InetSocketAddress address) throws IOException, InterruptedException {
        return sendTo(Utils.createSocket(dataStorage.getSSLContext(), address));
    }

    public T sendTo(SocketChannel socket) throws IOException, InterruptedException {
        socket.write(this.asByteBuffer());
        ByteBuffer response = readAllBytes(socket);
        socket.close();
        return parseResponse(response);
    }
}
