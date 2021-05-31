package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.PutProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class PutMessage extends DataStorageMessage<Boolean> {

    private final Chord.Key nodeKey;
    private final UUID id;
    private final byte[] data;

    public PutMessage(Chord.Key nodeKey, UUID id, byte[] data){
        this.nodeKey = nodeKey;
        this.id = id;
        this.data = data;
    }

    public PutMessage(Chord chord, byte[] data){
        int headerSize = Utils.find_nth(data, "\n".getBytes(), 1);
        String dataString = new String(data, 0, headerSize);
        String[] splitString = dataString.split(" ");
        nodeKey = chord.newKey(Long.parseLong(splitString[1]));
        id = new UUID(splitString[2]);

        int dataOffset = headerSize+1;
        this.data = new byte[data.length - dataOffset];
        System.arraycopy(data, dataOffset, this.data, 0, this.data.length);
    }

    @Override
    protected DataBuilder build() {
        return
            new DataBuilder(("PUT " + nodeKey + " " + id + "\n").getBytes())
            .append(data)
        ;
    }

    private static class PutProcessor extends Processor {

        private final PutMessage message;

        public PutProcessor(Chord chord, DataStorage dataStorage, SocketChannel socket, PutMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            PutProtocol putProtocol = new PutProtocol(getChord(), getDataStorage(), message.nodeKey, message.id, message.data);
            Boolean b = putProtocol.invoke();
            try {
                getSocket().write(message.formatResponse(b));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public PutProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new PutProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
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
