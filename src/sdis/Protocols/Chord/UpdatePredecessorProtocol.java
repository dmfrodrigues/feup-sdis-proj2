package sdis.Protocols.Chord;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class UpdatePredecessorProtocol extends ProtocolSupplier<Void> {

    private Chord chord;

    public UpdatePredecessorProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void get() {
        PeerInfo successor = chord.getSuccessor();
        try {
            Socket socket = chord.send(successor, new UpdatePredecessorMessage(chord.getPeerInfo()));
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
