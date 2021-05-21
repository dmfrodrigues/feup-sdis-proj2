package sdis.Modules.Main;

import sdis.Modules.Main.Messages.AuthenticateMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Utils.Pair;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class AuthenticationProtocol extends ProtocolSupplier<UserMetadata> {
    public static final int MAX_NUMBER_FUTURES = 10;

    private final Main main;
    private final Username username;
    private final Password password;

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
            Socket socket = systemStorage.sendAny(message);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            Pair<AuthenticateMessage.Status, UserMetadata> reply = message.parseResponse(response);
            switch(reply.first){
                case SUCCESS:
                    return reply.second;
                case NOTFOUND:

                    UserMetadata userMetadata = new UserMetadata(username, password);
                    Main.File file = userMetadata.asFile();
                    byte[] data = userMetadata.serialize();
                    BackupFileProtocol backupFileProtocol = new BackupFileProtocol(main, file, data, MAX_NUMBER_FUTURES);
                    if(!backupFileProtocol.get()) return null;
                    return userMetadata;

                case BROKEN:
                    return null;
            }
            return null;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
