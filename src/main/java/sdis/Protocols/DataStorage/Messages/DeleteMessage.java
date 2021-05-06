package sdis.Protocols.DataStorage.Messages;

import sdis.Chord;
import sdis.Protocols.DataStorage.DeleteProtocol;

import java.net.Socket;

public class DeleteMessage extends DataSystemMessage {

    private long key;

    public DeleteMessage(long key){
        this.key = key;
    }

    private long getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "DELETE " + getKey();
    }

    private static class DeleteProcessor extends Processor {

        private final DeleteMessage message;

        public DeleteProcessor(Chord chord, Socket socket, DeleteMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            DeleteProtocol deleteProtocol = new DeleteProtocol(getChord(), message.getKey());
            deleteProtocol.get();
            return null;
        }
    }

    @Override
    public DeleteProcessor getProcessor(Chord chord, Socket socket) {
        return new DeleteProcessor(chord, socket, this);
    }
}
