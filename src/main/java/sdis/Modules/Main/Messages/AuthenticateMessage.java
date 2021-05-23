package sdis.Modules.Main.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.*;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.Storage.ByteArrayChunkIterator;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;
import sdis.Utils.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.CompletionException;

import static sdis.Modules.Main.Main.USER_METADATA_REPDEG;

public class AuthenticateMessage extends MainMessage {
    public enum Status {
        SUCCESS,
        BROKEN
    }

    private final Username username;
    private final Password password;

    public AuthenticateMessage(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public AuthenticateMessage(byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        username = new Username(splitString[1]);
        password = new Password(splitString[2]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("AUTHENTICATE " + username + " " + password.getPlain()).getBytes());
    }

    private static class AuthenticateProcessor extends Processor {

        private final AuthenticateMessage message;

        public AuthenticateProcessor(Main main, Socket socket, AuthenticateMessage message){
            super(main, socket);
            this.message = message;
        }

        private boolean putUserMetadata(SystemStorage systemStorage, UserMetadata userMetadata) {
            int numChunks;
            byte[] data;
            try {
                data = userMetadata.serialize();
                ChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);
                numChunks = chunkIterator.length();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            Main.File file = message.username.asFile(numChunks);

            BackupFileProtocol backupFileProtocol = new BackupFileProtocol(getMain(), file, data, 10, false);
            boolean success = backupFileProtocol.get();
            if(!success){
                /*
                DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(getMain(), file, 10, false);
                deleteFileProtocol.get();
                 */
            }

            return success;
        }

        @Override
        public Void get() {
            SystemStorage systemStorage = getMain().getSystemStorage();
            Chord chord = systemStorage.getChord();

            Status status = Status.SUCCESS;
            UserMetadata userMetadata = null;

            DataBuilder builder = new DataBuilder();

            ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 1);
            RestoreUserFileProtocol restoreUserFileProtocol = new RestoreUserFileProtocol(getMain(), message.username, USER_METADATA_REPDEG, chunkOutput);
            boolean b = restoreUserFileProtocol.get();

            if(!b) {
                userMetadata = new UserMetadata(message.username, message.password);
                putUserMetadata(systemStorage, userMetadata);
                return get();
            } else {
                try {
                    byte[] data = builder.get();
                    InputStream is = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    userMetadata = (UserMetadata) ois.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                    status = Status.BROKEN;
                }
            }

            try {
                getSocket().getOutputStream().write(message.formatResponse(status, userMetadata));
                getSocket().shutdownOutput();
                getSocket().getInputStream().readAllBytes();
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            return null;
        }
    }

    @Override
    public AuthenticateProcessor getProcessor(Peer peer, Socket socket) {
        return new AuthenticateProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(Status status, UserMetadata userMetadata) {
        try {
            switch(status){
                case SUCCESS:
                    DataBuilder builder = new DataBuilder(new byte[]{0});
                    builder.append(userMetadata.serialize());
                    return builder.get();
                case BROKEN:
                    return new byte[]{1};
                default:
                    return new byte[]{1};
            }
        } catch (IOException e) {
            return new byte[]{2};
        }
    }

    public Pair<Status, UserMetadata> parseResponse(byte[] response) {
        try {
            switch(response[0]){
                case 0:
                    UserMetadata ret = UserMetadata.deserialize(response, 1, response.length-1);
                    return new Pair<>(Status.SUCCESS, ret);
                case 1:
                    return new Pair<>(Status.BROKEN, null);
                default:
                    return new Pair<>(Status.BROKEN, null);
            }
        } catch (IOException | ClassNotFoundException e) {
            return new Pair<>(Status.BROKEN, null);
        }
    }
}
