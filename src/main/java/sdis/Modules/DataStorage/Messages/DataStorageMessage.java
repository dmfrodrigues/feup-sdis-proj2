package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;

import java.net.Socket;
import java.util.function.Supplier;

public abstract class DataStorageMessage {
    public abstract String toString();

    public static abstract class Processor implements Supplier<Void> {
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

    public abstract DataStorageMessage.Processor getProcessor(Chord chord, DataStorage dataStorage, Socket socket);
}
