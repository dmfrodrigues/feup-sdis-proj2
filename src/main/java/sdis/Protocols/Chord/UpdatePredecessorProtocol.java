package sdis.Protocols.Chord;

import sdis.PeerInfo;
import sdis.Protocols.Chord.Messages.UpdatePredecessorMessage;
import sdis.Protocols.ProtocolSupplier;

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
