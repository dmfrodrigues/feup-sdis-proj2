package sdis.Modules.Chord;

import sdis.Modules.ProtocolTask;

public class LeaveProtocol extends ProtocolTask<Void> {
    private final Chord chord;
    private final ProtocolTask<Void> moveKeys;

    public LeaveProtocol(Chord chord, ProtocolTask<Void> moveKeys){
        this.chord = chord;
        this.moveKeys = moveKeys;
    }

    @Override
    public Void compute() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessor();

        // If it is alone, just leave
        if(r.equals(s)) return null;

        // Update predecessors and fingers tables of other nodes
        // Update predecessor of successor
        SetPredecessorProtocol setPredecessorProtocol = new SetPredecessorProtocol(chord, chord.getPredecessor());
        setPredecessorProtocol.invoke();
        // Update other nodes' fingers tables
        FingersRemoveProtocol fingersRemoveProtocol = new FingersRemoveProtocol(chord);
        fingersRemoveProtocol.invoke();
        // Move keys
        moveKeys.invoke();

        System.out.println("Peer " + chord.getKey() + " done leaving");

        return null;
    }
}
