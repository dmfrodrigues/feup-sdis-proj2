package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.UserMetadata;
import sdis.Modules.Main.Username;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DelistFileMessage extends AccountMessage<Boolean> {

    private final Main.File file;

    public DelistFileMessage(Main.File file){
        this.file = file;
    }

    public DelistFileMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        file = new Main.File(new Username(splitString[1]), new Main.Path(splitString[2]), 0, 0);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELISTFILE " + file.getOwner() + " " + file.getPath()).getBytes());
    }

    private static class DelistFileProcessor extends Processor {

        private final DelistFileMessage message;

        public DelistFileProcessor(Main main, Socket socket, DelistFileMessage message){
            super(main, socket);
            this.message = message;
        }

        private void end(boolean b) throws IOException, InterruptedException {
            getSocket().getOutputStream().write(message.formatResponse(b));
            readAllBytesAndClose(getSocket());
        }

        @Override
        public void compute() {
            Username owner = message.file.getOwner();

            try {
                // Get user metadata
                UserMetadata userMetadata = getUserMetadata(getMain(), owner);
                if(userMetadata == null){ end(false); return; }
                // Remove file
                userMetadata.removeFile(message.file.getPath());
                // Replace user metadata
                if(!replaceUserMetadata(getMain(), userMetadata)){ end(false); return; }

                try {
                    end(true);
                } catch (IOException | InterruptedException ex) {
                    throw new CompletionException(ex);
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                try {
                    end(false);
                } catch (IOException | InterruptedException ex) {
                    throw new CompletionException(ex);
                }
            }
        }
    }

    @Override
    public DelistFileProcessor getProcessor(Peer peer, Socket socket) {
        return new DelistFileProcessor(peer.getMain(), socket, this);
    }

    @Override
    protected byte[] formatResponse(Boolean b) {
        return new byte[]{(byte)(b ? 1 : 0)};
    }

    @Override
    public Boolean parseResponse(byte[] response) {
        return (response != null && response.length >= 1 && response[0] == 1);
    }
}
