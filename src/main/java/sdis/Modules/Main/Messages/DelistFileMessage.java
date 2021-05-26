package sdis.Modules.Main.Messages;

import sdis.Modules.Main.*;
import sdis.Peer;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DelistFileMessage extends MainMessage {

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

        private void end(boolean b) throws IOException {
            getSocket().getOutputStream().write(message.formatResponse(b));
            getSocket().shutdownOutput();
            getSocket().getInputStream().readAllBytes();
            getSocket().close();
        }

        @Override
        public Void get() {
            Username owner = message.file.getOwner();

            try {
                // Get user metadata
                DataBuilder builder = new DataBuilder();
                DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
                RestoreUserFileProtocol restoreUserFileProtocol = new RestoreUserFileProtocol(getMain(), owner, chunkOutput, 10);
                if(!restoreUserFileProtocol.invoke()){ end(false); return null; }

                // Parse user metadata
                byte[] data = builder.get();
                UserMetadata userMetadata = UserMetadata.deserialize(data);
                Main.File userMetadataFile = userMetadata.asFile();
                // Remove file
                userMetadata.removeFile(message.file.getPath());

                // Delete old user metadata
                DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(getMain(), userMetadataFile, 10, false);
                if(!deleteFileProtocol.invoke()){ end(false); return null; }

                // Save new user metadata
                data = userMetadata.serialize();
                BackupFileProtocol backupFileProtocol = new BackupFileProtocol(getMain(), userMetadataFile, data, false);
                if(!backupFileProtocol.invoke()){ end(false); return null; }

                try {
                    end(true);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }

            } catch (IOException | ClassNotFoundException e) { // InterruptedException | ExecutionException
                try {
                    end(false);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }

            return null;
        }
    }

    @Override
    public DelistFileProcessor getProcessor(Peer peer, Socket socket) {
        return new DelistFileProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        return new byte[]{(byte)(b ? 1 : 0)};
    }

    public boolean parseResponse(byte[] response) {
        return (response != null && response.length >= 1 && response[0] == 1);
    }
}
