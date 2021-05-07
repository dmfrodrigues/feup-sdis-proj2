package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DeleteProtocol;
import sdis.UUID;

import java.net.Socket;

public class DeleteMessage extends DataSystemMessage {

    private UUID id;

    public DeleteMessage(UUID id){
        this.id = id;
    }

    private UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "DELETE " + getId();
    }

    private static class DeleteProcessor extends Processor {

        private final DeleteMessage message;

        public DeleteProcessor(Chord chord, Socket socket, DeleteMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            DeleteProtocol deleteProtocol = new DeleteProtocol(getChord(), message.getId());
            deleteProtocol.get();
            return null;
        }
    }

    @Override
    public DeleteProcessor getProcessor(Chord chord, Socket socket) {
        return new DeleteProcessor(chord, socket, this);
    }
}