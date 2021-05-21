package sdis.Modules.Main.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.*;
import sdis.Peer;
import sdis.Storage.ChunkOutput;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;
import sdis.Utils.Pair;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletionException;

import static sdis.Modules.Main.AuthenticationProtocol.USER_METADATA_REPDEG;

public class AuthenticateMessage extends MainMessage {
    public enum Status {
        SUCCESS,
        NOTFOUND,
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

    private Username getUsername() {
        return username;
    }

    private Password getPassword(){
        return password;
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

        @Override
        public Void get() {
            Chord chord = getMain().getSystemStorage().getChord();
            Username username = message.getUsername();

            Status status = Status.SUCCESS;
            UserMetadata userMetadata = null;

            DataBuilder builder = new DataBuilder();

            ChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 1);
            RestoreUserFileProtocol restoreFileProtocol = new RestoreUserFileProtocol(getMain(), username, USER_METADATA_REPDEG, chunkOutput);
            if(!restoreFileProtocol.get()) {
                status = Status.NOTFOUND;
            } else {
                try {
                    byte[] data = builder.get();
                    InputStream is = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    userMetadata = (UserMetadata) ois.readObject();
                } catch (ClassNotFoundException | IOException e) {
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
                case NOTFOUND:
                    return new byte[]{1};
                case BROKEN:
                    return new byte[]{2};
                default:
                    return new byte[]{2};
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
                    return new Pair<>(Status.NOTFOUND, null);
                case 2:
                    return new Pair<>(Status.BROKEN, null);
                default:
                    return new Pair<>(Status.BROKEN, null);
            }
        } catch (IOException | ClassNotFoundException e) {
            return new Pair<>(Status.BROKEN, null);
        }
    }
}
