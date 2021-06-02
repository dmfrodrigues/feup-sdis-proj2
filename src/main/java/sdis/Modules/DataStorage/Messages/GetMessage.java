package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class GetMessage extends DataStorageMessage<byte[]> {

    private final UUID id;

    public GetMessage(UUID id){
        this.id = id;
    }

    public GetMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GET " + id).getBytes());
    }

    private static class GetProcessor extends Processor {

        private final GetMessage message;

        public GetProcessor(Chord chord, DataStorage dataStorage, SocketChannel socket, GetMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            GetProtocol getProtocol = new GetProtocol(getChord(), getDataStorage(), message.id);
            byte[] data = getProtocol.invoke();
            try {
                getSocket().write(message.formatResponse(data));
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new GetProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
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
