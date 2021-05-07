package sdis.Protocols.SystemStorage;

import sdis.Protocols.Chord.Chord;
import sdis.Peer;
import sdis.Protocols.Chord.FingersRemoveProtocol;
import sdis.Protocols.Chord.UpdatePredecessorProtocol;
import sdis.Protocols.ProtocolSupplier;

public class LeaveProtocol extends ProtocolSupplier<Void> {
    private final Peer peer;
    private final ProtocolSupplier<Void> moveKeys;

    public LeaveProtocol(Peer peer, ProtocolSupplier<Void> moveKeys){
        this.peer = peer;
        this.moveKeys = moveKeys;
    }

    @Override
    public Void get() {
        Chord chord = peer.getChord();
        long r = chord.getKey();

        // Update predecessors and fingers tables of other nodes
        // Update predecessor of successor
        UpdatePredecessorProtocol updatePredecessorProtocol = new UpdatePredecessorProtocol(chord, chord.getPredecessor());
        updatePredecessorProtocol.get();
        // Update other nodes' fingers tables
        FingersRemoveProtocol fingersRemoveProtocol = new FingersRemoveProtocol(chord);
        fingersRemoveProtocol.get();

        // Move keys
        moveKeys.get();

        return null;
    }
}
