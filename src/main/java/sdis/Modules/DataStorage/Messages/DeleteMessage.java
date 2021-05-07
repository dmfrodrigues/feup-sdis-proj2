package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.DeleteProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.net.Socket;

public class DeleteMessage extends DataStorageMessage {

    private final UUID id;

    public DeleteMessage(UUID id){
        this.id = id;
    }

    private UUID getId() {
        return id;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELETE " + getId()).getBytes());
    }

    private static class DeleteProcessor extends DataStorageMessage.Processor {

        private final DeleteMessage message;

        public DeleteProcessor(Chord chord, DataStorage dataStorage, Socket socket, DeleteMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            DeleteProtocol deleteProtocol = new DeleteProtocol(getChord(), getDataStorage(), message.getId());
            deleteProtocol.get();
            return null;
        }
    }

    @Override
    public DeleteProcessor getProcessor(Peer peer, Socket socket) {
        return new DeleteProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }
}
