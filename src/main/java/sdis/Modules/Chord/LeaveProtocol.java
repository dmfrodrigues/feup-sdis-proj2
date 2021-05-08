package sdis.Modules.Chord;

import sdis.Modules.ProtocolSupplier;

public class LeaveProtocol extends ProtocolSupplier<Void> {
    private final Chord chord;
    private final ProtocolSupplier<Void> moveKeys;

    public LeaveProtocol(Chord chord, ProtocolSupplier<Void> moveKeys){
        this.chord = chord;
        this.moveKeys = moveKeys;
    }

    @Override
    public Void get() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessor();

        // If it is alone, just leave
        if(r.equals(s)) return null;

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
