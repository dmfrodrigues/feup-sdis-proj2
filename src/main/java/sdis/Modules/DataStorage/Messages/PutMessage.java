package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.PutProtocol;
import sdis.UUID;

import java.net.Socket;

public class PutMessage extends DataStorageMessage {

    private final Chord.Key nodeKey;
    private final UUID id;
    private final byte[] data;

    public PutMessage(Chord.Key nodeKey, UUID id, byte[] data){
        this.nodeKey = nodeKey;
        this.id = id;
        this.data = data;
    }

    private Chord.Key getNodeKey() {
        return nodeKey;
    }

    private UUID getId() {
        return id;
    }

    private byte[] getData(){
        return data;
    }

    @Override
    public String toString() {
        return "PUT " + getNodeKey() + " " + getId() + "\n" + new String(getData());
    }

    private static class PutProcessor extends Processor {

        private final PutMessage message;

        public PutProcessor(Chord chord, DataStorage dataStorage, Socket socket, PutMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            PutProtocol putProtocol = new PutProtocol(getChord(), getDataStorage(), message.getNodeKey(), message.getId(), message.getData());
            putProtocol.get();
            return null;
        }
    }

    @Override
    public PutProcessor getProcessor(Chord chord, DataStorage dataStorage, Socket socket) {
        return new PutProcessor(chord, dataStorage, socket, this);
    }
}
