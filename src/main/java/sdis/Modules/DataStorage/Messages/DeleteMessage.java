package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.DeleteProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class DeleteMessage extends DataStorageMessage<Boolean> {

    private final UUID id;

    public DeleteMessage(UUID id){
        this.id = id;
    }

    public DeleteMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELETE " + id).getBytes());
    }

    private static class DeleteProcessor extends DataStorageMessage.Processor {

        private final DeleteMessage message;

        public DeleteProcessor(Chord chord, DataStorage dataStorage, SocketChannel socket, DeleteMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            DeleteProtocol deleteProtocol = new DeleteProtocol(getChord(), getDataStorage(), message.id);
            Boolean b = deleteProtocol.invoke();
            try {
                getSocket().write(message.formatResponse(b));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public DeleteProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new DeleteProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
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
