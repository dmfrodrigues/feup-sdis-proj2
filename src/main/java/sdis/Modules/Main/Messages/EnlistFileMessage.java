package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.UserMetadata;
import sdis.Modules.Main.Username;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class EnlistFileMessage extends AccountMessage<Boolean> {

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

        public EnlistFileProcessor(Main main, SocketChannel socket, EnlistFileMessage message){
            super(main, socket);
            this.message = message;
        }

        private void end(boolean b) throws IOException, InterruptedException {
            getSocket().write(message.formatResponse(b));
            readAllBytesAndClose(getSocket());
        }

        @Override
        public void compute() {
            Username owner = message.file.getOwner();

            try {
                // Get user metadata
                UserMetadata userMetadata = getUserMetadata(getMain(), owner);
                if(userMetadata == null){ end(false); return; }
                // Add file
                userMetadata.addFile(message.file);
                // Replace user metadata
                if(!replaceUserMetadata(getMain(), userMetadata)){ end(false); return; }

                try {
                    end(true);
                } catch (IOException | InterruptedException ex) {
                    throw new CompletionException(ex);
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                try {
                    e.printStackTrace();
                    end(false);
                } catch (IOException | InterruptedException ex) {
                    throw new CompletionException(ex);
                }
            }
        }
    }

    @Override
    public EnlistFileProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new EnlistFileProcessor(peer.getMain(), socket, this);
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
