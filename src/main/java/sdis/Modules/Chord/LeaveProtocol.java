package sdis.Modules.Chord;

import sdis.Modules.ProtocolSupplier;
import sdis.Peer;

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
        Chord.Key r = chord.getKey();

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
