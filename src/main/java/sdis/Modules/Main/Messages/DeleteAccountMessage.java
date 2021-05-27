package sdis.Modules.Main.Messages;

import sdis.Modules.Main.*;
import sdis.Modules.ProtocolTask;
import sdis.Peer;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class DeleteAccountMessage extends AccountMessage {
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
            boolean success = true;
            try {
                // Get user file
                UserMetadata userMetadata = Objects.requireNonNull(getUserMetadata(getMain(), message.username));

                // Delete all files
                Set<Main.Path> paths = userMetadata.getFiles();
                List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
                for (Main.Path p : paths) {
                    Main.File f = userMetadata.getFile(p);
                    tasks.add(new DeleteFileProtocol(getMain(), f));
                }

                invokeAll(tasks);
                for (RecursiveTask<Boolean> task : tasks) {
                    try {
                        success &= task.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        success = false;
                    }
                }

                // Delete user metadata
                getMain().deleteFile(userMetadata.asFile());

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
