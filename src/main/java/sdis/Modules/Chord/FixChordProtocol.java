package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.PredecessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class FixChordProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;

    public FixChordProtocol(Chord chord){
        this.chord = chord;
    }

    static public boolean fixSuccessors(Chord chord){
        Chord.NodeInfo n = chord.getNodeInfo();

        Chord.Key k = n.key;
        for(int i = 0; i < Chord.SUCCESSOR_LIST_SIZE; ++i) {
            Chord.NodeInfo s = chord.findSuccessor(k.add(1));
            if(s == null){
                System.err.println("Node " + n.key + ": Failed to find successor of " + k.add(1) + " to find one of its successors");
                return false;
            }
            if(s.equals(n)) break;
            chord.addSuccessor(s);
            k = s.key;
        }

        return true;
    }

    static public boolean fixFingers(Chord chord){
        boolean ret = true;

        Chord.NodeInfo n = chord.getNodeInfo();

        for(int i = 0; i < chord.getKeySize(); ++i){
            if(!chord.setFinger(i, chord.findSuccessor(n.key.add(1L << i)))) ret = false;
        }

        return ret;
    }

    @Override
    public Boolean compute() {
        // Update successors
        fixSuccessors(chord);
        // Update fingers
        fixFingers(chord);
        // Update predecessor
        Chord.NodeConn s = chord.getSuccessor();
        PredecessorMessage predecessorMessage = new PredecessorMessage();
        try {
            Chord.NodeInfo p = predecessorMessage.sendTo(chord, s.socket);
            chord.setPredecessor(p);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }

        return true;
    }
}
