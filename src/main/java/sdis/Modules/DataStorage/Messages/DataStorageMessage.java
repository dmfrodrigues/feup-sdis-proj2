package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.Peer;

import java.net.Socket;
import java.nio.channels.SocketChannel;

public abstract class DataStorageMessage extends Message {
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
}
