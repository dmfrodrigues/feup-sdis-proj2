package sdis.Modules.Chord;

import sdis.Modules.ProtocolTask;

public class LeaveProtocol extends ProtocolTask<Boolean> {
    private final Chord chord;
    private final ProtocolTask<Boolean> moveKeys;

    public LeaveProtocol(Chord chord, ProtocolTask<Boolean> moveKeys){
        this.chord = chord;
        this.moveKeys = moveKeys;
    }

    @Override
    public Boolean compute() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessorInfo();

        // If it is not alone, process stuff
        if(!r.equals(s)) {
            // Update predecessors and fingers tables of other nodes
            // Update predecessor of successor
            SetPredecessorProtocol setPredecessorProtocol = new SetPredecessorProtocol(chord, chord.getPredecessor());
            if (!setPredecessorProtocol.invoke()) return false;
            // Update other nodes' fingers tables
            FingersRemoveProtocol fingersRemoveProtocol = new FingersRemoveProtocol(chord);
            if (!fingersRemoveProtocol.invoke()) return false;
        }

        // Move keys
        if(!moveKeys.invoke()) return false;

        System.out.println("Peer " + chord.getKey() + " done leaving");

        return true;
    }
}
