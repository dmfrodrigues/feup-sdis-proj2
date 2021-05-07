package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetProtocol;
import sdis.UUID;

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

    private Chord.Key getNodeKey() {
        return nodeKey;
    }

    private UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "GET " + getNodeKey() + " " + getId();
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
    public GetProcessor getProcessor(Chord chord, DataStorage dataStorage, Socket socket) {
        return new GetProcessor(chord, dataStorage, socket, this);
    }
}
