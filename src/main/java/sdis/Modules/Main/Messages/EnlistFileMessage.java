package sdis.Modules.Main.Messages;

import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.Main;
import sdis.Modules.Main.UserMetadata;
import sdis.Modules.Main.Username;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

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
            Main main = getMain();
            SystemStorage systemStorage = main.getSystemStorage();
            DataStorage dataStorage = systemStorage.getDataStorage();
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();
            Username owner = message.file.getOwner();

            try {
                byte[] data = localDataStorage.get(owner.toUUID()).get();
                UserMetadata userMetadata = UserMetadata.deserialize(data);
                userMetadata.addFile(message.file);
                data = userMetadata.serialize();
                localDataStorage.put(owner.toUUID(), data).get();

                try {
                    getSocket().getOutputStream().write(message.formatResponse(true));
                    getSocket().shutdownOutput();
                    getSocket().getInputStream().readAllBytes();
                    getSocket().close();
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }

            } catch (InterruptedException | IOException | ClassNotFoundException | ExecutionException e) {
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
