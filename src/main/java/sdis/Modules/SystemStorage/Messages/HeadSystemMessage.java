package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class HeadSystemMessage extends SystemStorageMessage<Boolean> {

    private final UUID id;

    public HeadSystemMessage(UUID id){
        this.id = id;
    }

    public HeadSystemMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("HEADSYSTEM " + id).getBytes());
    }

    private static class GetSystemProcessor extends Processor {

        private final HeadSystemMessage message;

        public GetSystemProcessor(SystemStorage systemStorage, Socket socket, HeadSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean success = getSystemStorage().getDataStorage().head(message.id);

            try {
                getSocket().getOutputStream().write(message.formatResponse(success));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public GetSystemProcessor getProcessor(Peer peer, Socket socket) {
        return new GetSystemProcessor(peer.getSystemStorage(), socket, this);
    }

    @Override
    protected byte[] formatResponse(Boolean b) {
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    @Override
    public Boolean parseResponse(byte[] response) {
        return (response.length == 1 && response[0] == 1);
    }
}
