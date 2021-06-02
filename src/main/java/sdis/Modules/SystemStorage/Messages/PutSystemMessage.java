package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class PutSystemMessage extends SystemStorageMessage<Boolean> {

    private final UUID id;
    private final byte[] data;

    public PutSystemMessage(UUID id, byte[] data){
        this.id = id;
        this.data = data;
    }

    public PutSystemMessage(byte[] data){
        int headerSize = Utils.find_nth(data, "\n".getBytes(), 1);
        String dataString = new String(data, 0, headerSize);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);

        int dataOffset = headerSize+1;
        this.data = new byte[data.length - dataOffset];
        System.arraycopy(data, dataOffset, this.data, 0, this.data.length);
    }

    @Override
    protected DataBuilder build() {
        return
            new DataBuilder(("PUTSYSTEM " + id + "\n").getBytes())
            .append(data)
        ;
    }

    private static class PutSystemProcessor extends Processor {

        private final PutSystemMessage message;

        public PutSystemProcessor(SystemStorage systemStorage, SocketChannel socket, PutSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean b = getSystemStorage().getDataStorage().put(message.id, message.data);

                try {
                    getSocket().write(message.formatResponse(b));
                    getSocket().close();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
        }
    }

    @Override
    public PutSystemProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new PutSystemProcessor(peer.getSystemStorage(), socket, this);
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
