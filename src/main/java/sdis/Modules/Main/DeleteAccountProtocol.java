package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.DeleteAccountMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class DeleteAccountProtocol extends MainProtocolTask<Boolean> {

    private final Main main;
    private final Username username;
    private final Password password;

    public DeleteAccountProtocol(Main main, Username username, Password password){
        this.main = main;
        this.username = username;
        this.password = password;
    }

    @Override
    public Boolean compute() {
        Chord chord = main.getSystemStorage().getChord();
        UUID userMetadataFileUUID = username.asFile().getChunk(0).getReplica(0).getUUID();
        Chord.NodeInfo s = chord.getSuccessor(userMetadataFileUUID.getKey(chord));

        DeleteAccountMessage deleteAccountMessage = new DeleteAccountMessage(username, password);
        try {
            SocketChannel socket = main.send(s.address, deleteAccountMessage);

            byte[] data = readAllBytesAndClose(socket);
            return deleteAccountMessage.parseResponse(data);
        } catch (IOException | InterruptedException | UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();

            return false;
        }
    }
}
