package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class GetSystemMessage extends SystemStorageMessage<byte[]> {

    private final UUID id;

    public GetSystemMessage(UUID id){
        this.id = id;
    }

    public GetSystemMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GETSYSTEM " + id).getBytes());
    }

    private static class GetSystemProcessor extends Processor {

        private final GetSystemMessage message;

        public GetSystemProcessor(SystemStorage systemStorage, SocketChannel socket, GetSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            byte[] data = getSystemStorage().getDataStorage().get(message.id);

            try {
                getSocket().write(message.formatResponse(data));
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
    protected ByteBuffer formatResponse(byte[] data) {
        if(data == null) return ByteBuffer.wrap(new byte[]{0});
        byte[] ret = new byte[data.length+1];
        ret[0] = 1;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ByteBuffer.wrap(ret);
    }

    @Override
    public byte[] parseResponse(ByteBuffer response) {
        if(response.array()[0] == 0) return null;
        else {
            byte[] ret = new byte[response.position()-1];
            System.arraycopy(response.array(), 1, ret, 0, ret.length);
            return ret;
        }
    }
}
