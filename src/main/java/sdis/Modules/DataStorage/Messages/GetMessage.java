package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetMessage extends DataStorageMessage {

    private final Chord.Key nodeKey;
    private final UUID id;

    public GetMessage(Chord.Key nodeKey, UUID id){
        this.nodeKey = nodeKey;
        this.id = id;
    }

    public GetMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        nodeKey = new Chord.Key(Long.parseLong(splitString[1]));
        id = new UUID(splitString[2]);
    }

    private Chord.Key getNodeKey() {
        return nodeKey;
    }

    private UUID getId() {
        return id;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GET " + getNodeKey() + " " + getId()).getBytes());
    }

    private static class GetProcessor extends Processor {

        private final GetMessage message;

        public GetProcessor(Chord chord, DataStorage dataStorage, Socket socket, GetMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            GetProtocol getProtocol = new GetProtocol(getChord(), getDataStorage(), message.getNodeKey(), message.getId());
            byte[] data = getProtocol.get();
            try {
                getSocket().getOutputStream().write(data);
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            return null;
        }
    }

    @Override
    public GetProcessor getProcessor(Peer peer, Socket socket) {
        return new GetProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }
}
