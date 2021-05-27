package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class PutSystemMessage extends SystemStorageMessage {

    private final UUID id;
    private final byte[] data;

    public PutSystemMessage(UUID id, byte[] data){
        this.id = id;
        this.data = data;
    }

    public PutSystemMessage(byte[] data){
        int headerSize = Utils.find_nth(data, "\n".getBytes(), 1);
        String dataString = new String(data, 0, headerSize);
        String[] splitString = dataString.split(" ");
        id = new UUID(splitString[1]);

        int dataOffset = headerSize+1;
        this.data = new byte[data.length - dataOffset];
        System.arraycopy(data, dataOffset, this.data, 0, this.data.length);
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
            new DataBuilder(("PUTSYSTEM " + getId() + "\n").getBytes())
            .append(getData())
        ;
    }

    private static class PutSystemProcessor extends Processor {

        private final PutSystemMessage message;

        public PutSystemProcessor(SystemStorage systemStorage, Socket socket, PutSystemMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            boolean b = getSystemStorage().getDataStorage().put(message.getId(), message.getData());

                try {
                    getSocket().getOutputStream().write(message.formatResponse(b));
                    readAllBytesAndClose(getSocket());
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
        }
    }

    @Override
    public PutSystemProcessor getProcessor(Peer peer, Socket socket) {
        return new PutSystemProcessor(peer.getSystemStorage(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        byte[] ret = new byte[1];
        ret[0] = (byte) (b ? 1 : 0);
        return ret;
    }

    public boolean parseResponse(byte[] response) {
        return (response[0] != 0);
    }
}
