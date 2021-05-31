package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class HeadSystemMessage extends SystemStorageMessage<Boolean> {

    private final UUID id;

    public HeadSystemMessage(UUID id){
        this.id = id;
    }

    public HeadSystemMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("HEADSYSTEM " + id).getBytes());
    }

    private static class GetSystemProcessor extends Processor {

        private final HeadSystemMessage message;

        public GetSystemProcessor(SystemStorage systemStorage, SocketChannel socket, HeadSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean success = getSystemStorage().getDataStorage().head(message.id);

            try {
                getSocket().write(message.formatResponse(success));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetSystemProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new GetSystemProcessor(peer.getSystemStorage(), socket, this);
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
