package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.PutProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class PutMessage extends DataStorageMessage {

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

    private Chord.Key getNodeKey() {
        return nodeKey;
    }

    private UUID getId() {
        return id;
    }

    private byte[] getData(){
        return data;
    }

    @Override
    protected DataBuilder build() {
        return
            new DataBuilder(("PUT " + getNodeKey() + " " + getId() + "\n").getBytes())
            .append(getData())
        ;
    }

    private static class PutProcessor extends Processor {

        private final PutMessage message;

        public PutProcessor(Chord chord, DataStorage dataStorage, Socket socket, PutMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            PutProtocol putProtocol = new PutProtocol(getChord(), getDataStorage(), message.getNodeKey(), message.getId(), message.getData());
            Boolean b = putProtocol.get();
            try {
                getSocket().getOutputStream().write(b ? 1 : 0);
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
    public PutProcessor getProcessor(Peer peer, Socket socket) {
        return new PutProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }
}
