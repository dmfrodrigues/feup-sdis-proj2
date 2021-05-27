package sdis.Modules.Main.Messages;

import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.*;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;
import sdis.Utils.Pair;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class DeleteAccountMessage extends MainMessage {
    private final Username username;
    private final Password password;

    public DeleteAccountMessage(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public DeleteAccountMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        username = new Username(splitString[1]);
        password = new Password(splitString[2]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELETEACCOUNT " + username + " " + password.getPlain()).getBytes());
    }

    private static class DeleteAccountProcessor extends Processor {

        private final DeleteAccountMessage message;

        public DeleteAccountProcessor(Main main, Socket socket, DeleteAccountMessage message){
            super(main, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            Main.File userMetadataFile = message.username.asFile();

            boolean success = true;
            try {
                // Get user file
                DataBuilder dataBuilder = new DataBuilder();
                ChunkOutput chunkOutput = new DataBuilderChunkOutput(dataBuilder, 10);
                getMain().restoreFile(userMetadataFile, chunkOutput);
                byte[] data = dataBuilder.get();
                UserMetadata userMetadata = UserMetadata.deserialize(data);

                // Delete user metadata
                getMain().deleteFile(userMetadata.asFile());

                // Delete all files
                Set<Main.Path> paths = userMetadata.getFiles();
                List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
                for (Main.Path p : paths) {
                    Main.File f = userMetadata.getFile(p);
                    tasks.add(new DeleteFileProtocol(getMain(), f, 10));
                }

                invokeAll(tasks);
                for (RecursiveTask<Boolean> task : tasks) {
                    try {
                        success &= task.get();
                    } catch (InterruptedException | ExecutionException e) {
                        success = false;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                success = false;
            }

            try {
                getSocket().getOutputStream().write(message.formatResponse(success));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public DeleteAccountProcessor getProcessor(Peer peer, Socket socket) {
        return new DeleteAccountProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    public boolean parseResponse(byte[] response) {
        return (response[0] == 1);
    }
}
