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
            UUID id = message.getUsername().getId();

            try {
                byte[] file = new RestoreFile(id).get();

                InputStream is = new ByteArrayInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(is);
                UserMetadata userMetadata = (UserMetadata) ois.readObject();
                ois.close();
                is.close();

                if (!userMetadata.getPassword().authenticate(message.password)) {
                    userMetadata = null;
                }

                getSocket().getOutputStream().write(message.formatResponse(userMetadata));
                getSocket().shutdownOutput();
                getSocket().getInputStream().readAllBytes();
                getSocket().close();
            } catch (IOException | ClassNotFoundException e) {
                throw new CompletionException(e);
            }

            return null;
        }
    }

    @Override
    public AuthenticateProcessor getProcessor(Peer peer, Socket socket) {
        return new AuthenticateProcessor(peer.getMain(), socket, this);
    }

    private byte[] formatResponse(UserMetadata userMetadata) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(userMetadata);
            oos.close();
            os.close();
            DataBuilder builder = new DataBuilder(new byte[]{1});
            builder.append(os.toByteArray());
            return builder.get();
        } catch (IOException e) {
            return new byte[]{0};
        }
    }

    public UserMetadata parseResponse(byte[] response) {
        try {
            if(response[0] != 1) return null;
            InputStream is = new ByteArrayInputStream(response, 1, response.length-1);
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
