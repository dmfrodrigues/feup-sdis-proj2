package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DeleteSystemMessage extends SystemStorageMessage {

    private final UUID id;

    public DeleteSystemMessage(UUID id){
        this.id = id;
    }

    public DeleteSystemMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    private UUID getId() {
        return id;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELETESYSTEM " + getId()).getBytes());
    }

    private static class DeleteSystem extends Processor {

        private final DeleteSystemMessage message;

        public DeleteSystem(SystemStorage systemStorage, Socket socket, DeleteSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean b = getSystemStorage().getDataStorage().delete(message.getId());
            try {
                getSocket().getOutputStream().write(message.formatResponse(b));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public DeleteSystem getProcessor(Peer peer, Socket socket) {
        return new DeleteSystem(peer.getSystemStorage(), socket, this);
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
