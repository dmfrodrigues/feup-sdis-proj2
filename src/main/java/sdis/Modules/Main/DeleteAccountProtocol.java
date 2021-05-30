package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Main.Messages.DeleteAccountMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;

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
        Chord.NodeInfo s = chord.findSuccessor(userMetadataFileUUID.getKey(chord));

        DeleteAccountMessage deleteAccountMessage = new DeleteAccountMessage(username, password);
        try {
            return deleteAccountMessage.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }
    }
}
