package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class DeleteSystemMessage extends SystemStorageMessage<Boolean> {

    private final UUID id;

    public DeleteSystemMessage(UUID id){
        this.id = id;
    }

    public DeleteSystemMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELETESYSTEM " + id).getBytes());
    }

    private static class DeleteSystem extends Processor {

        private final DeleteSystemMessage message;

        public DeleteSystem(SystemStorage systemStorage, SocketChannel socket, DeleteSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean b = getSystemStorage().getDataStorage().delete(message.id);
            try {
                getSocket().write(message.formatResponse(b));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public DeleteSystem getProcessor(Peer peer, SocketChannel socket) {
        return new DeleteSystem(peer.getSystemStorage(), socket, this);
    }

    @Override
    protected ByteBuffer formatResponse(Boolean b) {
        return ByteBuffer.wrap(new byte[]{(byte) (b ? 1 : 0)});
    }

    @Override
    public Boolean parseResponse(ByteBuffer response) {
        return (response.position() == 1 && response.array()[0] == 1);
    }
}
