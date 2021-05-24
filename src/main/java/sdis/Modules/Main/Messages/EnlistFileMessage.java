package sdis.Modules.Main.Messages;

import sdis.Modules.Main.*;
import sdis.Peer;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

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
                if(!restoreUserFileProtocol.get()){ end(false); return null; }

                // Parse user metadata
                byte[] data = builder.get();
                UserMetadata userMetadata = UserMetadata.deserialize(data);
                Main.File userMetadataFile = userMetadata.asFile();
                // Add file
                userMetadata.addFile(message.file);

                // Delete old user metadata
                DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(getMain(), userMetadataFile, 10, false);
                if(!deleteFileProtocol.get()){ end(false); return null; }

                // Save new user metadata
                data = userMetadata.serialize();
                BackupFileProtocol backupFileProtocol = new BackupFileProtocol(getMain(), userMetadataFile, data, 10, false);
                if(!backupFileProtocol.get()){ end(false); return null; }

                try {
                    end(true);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }

            } catch (IOException | ClassNotFoundException e) { // InterruptedException | ExecutionException
                try {
                    e.printStackTrace();
                    end(false);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }

            return null;
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
