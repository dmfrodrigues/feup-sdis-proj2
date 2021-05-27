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

    private final UUID id;

    public GetMessage(UUID id){
        this.id = id;
    }

    public GetMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    private UUID getId() {
        return id;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GET " + getId()).getBytes());
    }

    private static class GetProcessor extends Processor {

        private final GetMessage message;

        public GetProcessor(Chord chord, DataStorage dataStorage, Socket socket, GetMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            GetProtocol getProtocol = new GetProtocol(getChord(), getDataStorage(), message.getId());
            byte[] data = getProtocol.invoke();
            try {
                getSocket().getOutputStream().write(message.formatResponse(data));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
            return null;
        }
    }

    @Override
    public GetProcessor getProcessor(Peer peer, Socket socket) {
        return new GetProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }

    private byte[] formatResponse(byte[] data) {
        if(data == null) return new byte[]{0};
        byte[] ret = new byte[data.length+1];
        ret[0] = 1;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ret;
    }

    public byte[] parseResponse(byte[] response) {
        if(response[0] == 0) return null;
        else {
            byte[] ret = new byte[response.length-1];
            System.arraycopy(response, 1, ret, 0, ret.length);
            return ret;
        }
    }
}
