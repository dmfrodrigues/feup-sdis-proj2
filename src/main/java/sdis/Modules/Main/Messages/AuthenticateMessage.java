package sdis.Modules.Main.Messages;

import sdis.Modules.Main.*;
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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class AuthenticateMessage extends MainMessage<Pair<AuthenticateMessage.Status, UserMetadata>> {
    public enum Status {
        SUCCESS,
        BROKEN,
        UNAUTHORIZED
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

        public AuthenticateProcessor(Main main, SocketChannel socket, AuthenticateMessage message){
            super(main, socket);
            this.message = message;
        }

        private boolean putUserMetadata(UserMetadata userMetadata) {
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

            BackupFileProtocol backupFileProtocol = new BackupFileProtocol(getMain(), file, data, false);
            boolean success = backupFileProtocol.invoke();
            if(!success){
                DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(getMain(), file, false);
                deleteFileProtocol.invoke();
            }

            return success;
        }

        @Override
        public void compute() {
            Status status = Status.SUCCESS;
            UserMetadata userMetadata = null;

            DataBuilder builder = new DataBuilder();

            ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 1);
            RestoreUserFileProtocol restoreUserFileProtocol = new RestoreUserFileProtocol(getMain(), message.username, chunkOutput);
            boolean b = restoreUserFileProtocol.invoke();

            if(!b) {
                userMetadata = new UserMetadata(message.username, message.password);
                if(putUserMetadata(userMetadata)){ compute(); return; }
                status = Status.BROKEN;
            } else {
                try {
                    byte[] data = builder.get();
                    InputStream is = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    userMetadata = (UserMetadata) ois.readObject();
                    if(!userMetadata.getPassword().authenticate(message.password)){
                        status = Status.UNAUTHORIZED;
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                    status = Status.BROKEN;
                }
            }

            try {
                getSocket().write(message.formatResponse(new Pair<>(status, userMetadata)));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public AuthenticateProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new AuthenticateProcessor(peer.getMain(), socket, this);
    }

    @Override
    protected ByteBuffer formatResponse(Pair<Status, UserMetadata> p) {
        try {
            switch(p.first){
                case SUCCESS:
                    DataBuilder builder = new DataBuilder(new byte[]{0});
                    builder.append(p.second.serialize());
                    return ByteBuffer.wrap(builder.get());
                case BROKEN:
                    return ByteBuffer.wrap(new byte[]{1});
                case UNAUTHORIZED:
                default:
                    return ByteBuffer.wrap(new byte[]{2});
            }
        } catch (IOException e) {
            return ByteBuffer.wrap(new byte[]{2});
        }
    }

    @Override
    public Pair<Status, UserMetadata> parseResponse(ByteBuffer byteBuffer) {
        byte[] response = new byte[byteBuffer.position()];
        System.arraycopy(byteBuffer.array(), 0, response, 0, response.length);

        try {
            switch(response[0]){
                case 0:
                    UserMetadata ret = UserMetadata.deserialize(response, 1, response.length-1);
                    return new Pair<>(Status.SUCCESS, ret);
                case 1:
                    return new Pair<>(Status.BROKEN, null);
                case 2:
                default:
                    return new Pair<>(Status.UNAUTHORIZED, null);
            }
        } catch (IOException | ClassNotFoundException e) {
            return new Pair<>(Status.BROKEN, null);
        }
    }
}
