package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.AuthenticateMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class AuthenticationProtocol extends ProtocolSupplier<UserMetadata> {

    private final Main main;
    private Username username;
    private Password password;

    public AuthenticationProtocol(Main main, Username username, Password password){
        this.main = main;
        this.username = username;
        this.password = password;
    }

    @Override
    public UserMetadata get() {
        SystemStorage systemStorage = main.getSystemStorage();

        try {
            AuthenticateMessage message = new AuthenticateMessage(username, password);
            Socket socket = systemStorage.forward(username.getId(), message);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return message.parseResponse(response);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
