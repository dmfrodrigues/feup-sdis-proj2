package sdis.Modules.Main.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetProtocol;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.Main;
import sdis.Modules.Main.Password;
import sdis.Modules.Main.UserMetadata;
import sdis.Modules.Main.Username;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class AuthenticateMessage extends MainMessage {

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
            LocalDataStorage localDataStorage = getMain().getSystemStorage().getDataStorage().getLocalDataStorage();

            UUID id = message.getUsername().getId();
            localDataStorage.get(id)
            .thenApplyAsync((byte[] data) -> {
                try {
                    getSocket().getOutputStream().write(message.formatResponse(data));
                    getSocket().shutdownOutput();
                    getSocket().getInputStream().readAllBytes();
                    getSocket().close();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
                return null;
            });

            return null;
        }
    }

    @Override
    public AuthenticateProcessor getProcessor(Peer peer, Socket socket) {
        return new AuthenticateProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(byte[] data) {
        return data;
    }

    public UserMetadata parseResponse(byte[] response) {
        try {
            InputStream is = new ByteArrayInputStream(response);
            ObjectInputStream ois = new ObjectInputStream(is);
            UserMetadata ret = (UserMetadata) ois.readObject();
            ois.close();
            is.close();
            return ret;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
