package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.UnnotifySuccessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;

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
            SetPredecessorProtocol setPredecessorProtocol = new SetPredecessorProtocol(chord, chord.getPredecessorInfo());
            if (!setPredecessorProtocol.invoke()) return false;
            // Update other nodes' fingers tables
            FingersRemoveProtocol fingersRemoveProtocol = new FingersRemoveProtocol(chord);
            if (!fingersRemoveProtocol.invoke()) return false;
            // Update other nodes' successors lists
            Chord.NodeInfo p = chord.getPredecessorInfo();
            for(int i = 0; i < Chord.SUCCESSOR_LIST_SIZE && !p.equals(r); ++i){
                UnnotifySuccessorMessage unnotifySuccessorMessage = new UnnotifySuccessorMessage(r);
                try {
                    if(!unnotifySuccessorMessage.sendTo(chord, p.address))
                        System.err.println("Node " + r.key + ": Failed to unnotify " + p.key + "; proceeding as usual");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
                p = chord.findPredecessor(p.key);
            }
        }

        // Move keys
        if(!moveKeys.invoke()) return false;

        return true;
    }
}
