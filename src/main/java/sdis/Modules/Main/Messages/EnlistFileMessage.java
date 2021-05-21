package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.Username;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.Socket;

public class EnlistFileMessage extends MainMessage {

    private final Main.File file;

    public EnlistFileMessage(Main.File file){
        this.file = file;
    }

    public EnlistFileMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        Username username = new Username(splitString[1]);
        Main.Path path = new Main.Path(splitString[2]);
        long numChunks = Integer.parseInt(splitString[3]);
        int repDegree = Integer.parseInt(splitString[4]);
        file = new Main.File(username, path, numChunks, repDegree);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("ENLISTFILE " + file.getOwner() + " " + file.getPath() + " " + file.getNumberOfChunks() + " " + file.getReplicationDegree()).getBytes());
    }

    private static class EnlistFileProcessor extends Processor {

        private final EnlistFileMessage message;

        public EnlistFileProcessor(Main main, Socket socket, EnlistFileMessage message){
            super(main, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            
        }
    }

    @Override
    public EnlistFileProcessor getProcessor(Peer peer, Socket socket) {
        return new EnlistFileProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        return new byte[]{(byte)(b ? 1 : 0)};
    }

    public boolean parseResponse(byte[] response) {
        return (response != null && response.length >= 1 && response[0] == 1);
    }
}
