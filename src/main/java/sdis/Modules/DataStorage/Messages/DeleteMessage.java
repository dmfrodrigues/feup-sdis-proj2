package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.DeleteProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DeleteMessage extends DataStorageMessage {

    private final UUID id;

    public DeleteMessage(UUID id){
        this.id = id;
    }

    public DeleteMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
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
            Boolean b = deleteProtocol.get();
            try {
                getSocket().getOutputStream().write(message.formatResponse(b));
                getSocket().shutdownOutput();
                getSocket().getInputStream().readAllBytes();
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            return null;
        }
    }

    @Override
    public DeleteProcessor getProcessor(Peer peer, Socket socket) {
        return new DeleteProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        byte[] ret = new byte[1];
        ret[0] = (byte) (b ? 1 : 0);
        return ret;
    }

    public boolean parseResponse(byte[] response) {
        return (response[0] != 0);
    }
}
