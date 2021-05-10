package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetProtocol;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetMessage extends DataStorageMessage {

    private final UUID id;

    public GetMessage(UUID id){
        this.id = id;
    }

    public GetMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[2]);
    }

    private UUID getId() {
        return id;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GET " + getId()).getBytes());
    }

    public byte[] parseResponse(byte[] response) {
        if(response[0] == 0) return null;
        else {
            byte[] ret = new byte[response.length-1];
            System.arraycopy(response, 1, ret, 0, ret.length);
            return ret;
        }
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
            byte[] data = getProtocol.get();
            try {
                OutputStream os = getSocket().getOutputStream();
                if(data == null) os.write(0);
                else {
                    os.write(1);
                    os.write(data);
                }
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
