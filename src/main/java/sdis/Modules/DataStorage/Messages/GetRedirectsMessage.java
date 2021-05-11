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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class GetRedirectsMessage extends DataStorageMessage {

    public GetRedirectsMessage(){
    }

    public GetRedirectsMessage(byte[] data){
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder("GETREDIRECTS".getBytes());
    }

    private static class GetRedirectsProcessor extends Processor {

        private final GetRedirectsMessage message;

        public GetRedirectsProcessor(Chord chord, DataStorage dataStorage, Socket socket, GetRedirectsMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            Set<UUID> ids = getDataStorage().getRedirects();
            try {
                OutputStream os = getSocket().getOutputStream();
                os.write(message.formatResponse(ids));
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            return null;
        }
    }

    @Override
    public GetRedirectsProcessor getProcessor(Peer peer, Socket socket) {
        return new GetRedirectsProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }

    public byte[] formatResponse(Set<UUID> ids) {
        DataBuilder builder = new DataBuilder();
        for(UUID id: ids){
            builder.append((id.toString() + "\n").getBytes());
        }
        return builder.get();
    }

    public Set<UUID> parseResponse(byte[] data) {
        String s = new String(data);
        String[] split = s.split("\n");
        Set<UUID> ret = new HashSet<>();
        for(String idString: split){
            ret.add(new UUID(idString));
        }
        return ret;
    }
}