package sdis.Modules.Main.Messages;

import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.*;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class DelistFileMessage extends MainMessage {

    private final Username owner;
    private final Main.Path path;

    public DelistFileMessage(Username owner, Main.Path path){
        this.owner = owner;
        this.path = path;
    }

    public DelistFileMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        owner = new Username(splitString[1]);
        path = new Main.Path(splitString[2]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("DELISTFILE " + owner + " " + path).getBytes());
    }

    private static class DelistFileProcessor extends Processor {

        private final DelistFileMessage message;

        public DelistFileProcessor(Main main, Socket socket, DelistFileMessage message){
            super(main, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            Main main = getMain();
            SystemStorage systemStorage = main.getSystemStorage();
            DataStorage dataStorage = systemStorage.getDataStorage();
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();
            Username owner = message.owner;
            Main.File file = message.owner.asFile();

            try {
                DataBuilder builder = new DataBuilder();
                DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
                RestoreFileProtocol restoreFileProtocol = new RestoreFileProtocol(getMain(), file, chunkOutput, 10);
                restoreFileProtocol.get();
                byte[] data = builder.get();
                UserMetadata userMetadata = UserMetadata.deserialize(data);
                userMetadata.removeFile(message.path);
                data = userMetadata.serialize();
//                DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(getMain(), file, 10, false);
//                deleteFileProtocol.get();
                BackupFileProtocol backupFileProtocol = new BackupFileProtocol(getMain(), file, data, 10, false);
                backupFileProtocol.get();

                try {
                    getSocket().getOutputStream().write(message.formatResponse(true));
                    getSocket().shutdownOutput();
                    getSocket().getInputStream().readAllBytes();
                    getSocket().close();
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }

            } catch (IOException | ClassNotFoundException e) { // InterruptedException | ExecutionException
                try {
                    getSocket().getOutputStream().write(message.formatResponse(false));
                    getSocket().shutdownOutput();
                    getSocket().getInputStream().readAllBytes();
                    getSocket().close();
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
