package sdis.Modules.Main;

import sdis.Modules.Main.Messages.AuthenticateMessage;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Utils.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class AuthenticationProtocol extends MainProtocolTask<UserMetadata> {
    private final Main main;
    private final Username username;
    private final Password password;

    public AuthenticationProtocol(Main main, Username username, Password password){
        this.main = main;
        this.username = username;
        this.password = password;
    }

    @Override
    public UserMetadata compute() {
        SystemStorage systemStorage = main.getSystemStorage();

        try {
            AuthenticateMessage message = new AuthenticateMessage(username, password);
            SocketChannel socket = systemStorage.sendAny(message);
            ByteBuffer response = readAllBytes(socket);
            socket.close();
            Pair<AuthenticateMessage.Status, UserMetadata> reply = message.parseResponse(response);
            switch(reply.first){
                case SUCCESS: return reply.second;
                case BROKEN : return null;
                default     : return null;
            }
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
}
