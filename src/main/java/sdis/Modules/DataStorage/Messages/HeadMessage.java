package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.HeadProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class HeadMessage extends DataStorageMessage<Boolean> {

    private final UUID id;

    public HeadMessage(UUID id){
        this.id = id;
    }

    public HeadMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("HEAD " + id).getBytes());
    }

    private static class HeadProcessor extends Processor {

        private final HeadMessage message;

        public HeadProcessor(Chord chord, DataStorage dataStorage, SocketChannel socket, HeadMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            HeadProtocol getProtocol = new HeadProtocol(getChord(), getDataStorage(), message.id);
            boolean success = getProtocol.invoke();
            try {
                getSocket().write(message.formatResponse(success));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public HeadProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new HeadProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
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
