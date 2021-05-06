package sdis.Protocols.DataStorage;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.DataStorage.Messages.DeleteMessage;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DeleteProtocol extends ProtocolSupplier<Integer> {

    private final Chord chord;
    private long key;

    public DeleteProtocol(Chord chord, long key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public Integer get() {
        PeerInfo r = chord.getPeerInfo();
        PeerInfo s = chord.getSuccessor();

        boolean hasStored = false; // TODO: Fix boolean
        boolean pointsToSuccessor = false; // TODO: Fix condition
        // If r has not stored that datapiece and has no pointer saying its successor stored it
        if(!hasStored && !pointsToSuccessor){
            return 0;
        }
        // If r has stored that datapiece
        if(hasStored) {
            // TODO: Delete the datapiece
            return 0;
        }
        // If r has not stored that datapiece but has a pointer to its successor reporting that it might have stored
        if(!hasStored && pointsToSuccessor) {
            try {
                Socket socket = chord.send(s, new DeleteMessage(key));
                int response = Integer.parseInt(new String(socket.getInputStream().readAllBytes()));
                return response;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }

        return 0;
    }
}
