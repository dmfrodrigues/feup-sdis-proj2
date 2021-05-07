package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.PeerInfo;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DeleteProtocol extends ProtocolSupplier<Boolean> {

    private final Chord chord;
    private final UUID id;

    public DeleteProtocol(Chord chord, UUID id){
        this.chord = chord;
        this.id = id;
    }

    @Override
    public Boolean get() {
        PeerInfo r = chord.getPeerInfo();
        PeerInfo s = chord.getSuccessor();

        boolean hasStored = false; // TODO: Fix boolean
        boolean pointsToSuccessor = false; // TODO: Fix condition
        // If r has not stored that datapiece and has no pointer saying its successor stored it
        if(!hasStored && !pointsToSuccessor){
            return true;
        }
        // If r has stored that datapiece
        if(hasStored) {
            // TODO: Delete the datapiece
            return true;
        }
        // If r has not stored that datapiece but has a pointer to its successor reporting that it might have stored
        if(!hasStored && pointsToSuccessor) {
            try {
                Socket socket = chord.send(s, new DeleteMessage(id));
                return Boolean.parseBoolean(new String(socket.getInputStream().readAllBytes()));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }

        return true;
    }
}
