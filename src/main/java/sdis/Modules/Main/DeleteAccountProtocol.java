package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.DeleteAccountMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;

public class DeleteAccountProtocol extends ProtocolTask<Boolean> {

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
            Socket socket = main.send(s.address, deleteAccountMessage);
            byte[] data = readAllBytesAndClose(socket);
            return deleteAccountMessage.parseResponse(data);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }
    }
}
