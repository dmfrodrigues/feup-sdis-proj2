package sdis.Modules.Chord;

import sdis.Modules.Chord.Chord;
import sdis.PeerInfo;
import sdis.Modules.Chord.Messages.UpdatePredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class UpdatePredecessorProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;
    private final PeerInfo peerInfo;

    public UpdatePredecessorProtocol(Chord chord, PeerInfo peerInfo){
        this.chord = chord;
        this.peerInfo = peerInfo;
    }

    @Override
    public Void get() {
        PeerInfo successor = chord.getSuccessor();
        try {
            Socket socket = chord.send(successor, new UpdatePredecessorMessage(peerInfo));
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
