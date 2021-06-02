package sdis.Modules.Chord;

import sdis.Modules.Chord.Exceptions.KeyAlreadyExistsException;

import sdis.Modules.Chord.Messages.FindSuccessorMessage;
import sdis.Modules.Chord.Messages.NotifySuccessorMessage;
import sdis.Modules.Chord.Messages.PredecessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletionException;

public class JoinProtocol extends ProtocolTask<Boolean> {
    private final Chord chord;
    private final InetSocketAddress g;
    private final ProtocolTask<Boolean> moveKeys;

    public JoinProtocol(Chord chord, InetSocketAddress gateway, ProtocolTask<Boolean> moveKeys){
        this.chord = chord;
        this.g = gateway;
        this.moveKeys = moveKeys;
    }

    @Override
    public Boolean compute() {
        System.out.println("Node " + chord.getNodeInfo().key + ": Starting to join");

        Chord.NodeInfo n = chord.getNodeInfo();

        // Initialize fingers table and predecessor
        // Get predecessor
        try {
            // Get successors
            Chord.Key k = n.key;
            FindSuccessorMessage findSuccessorMessage = new FindSuccessorMessage(k.add(1));
            Chord.NodeInfo s = findSuccessorMessage.sendTo(chord, g);
            chord.addSuccessor(s);
            k = s.key;
            for(int i = 1; i < Chord.SUCCESSOR_LIST_SIZE; ++i) {
                findSuccessorMessage = new FindSuccessorMessage(k.add(1));
                Chord.NodeInfo t = findSuccessorMessage.sendTo(chord, g);
                if(t.equals(s)){
                    break;
                }
                chord.addSuccessor(t);
                k = t.key;
            }
            chord.setFinger(0, chord.getSuccessorInfo());
            // Get predecessor
            PredecessorMessage predecessorMessage = new PredecessorMessage();
            chord.setPredecessor(predecessorMessage.sendTo(chord, s.address));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        // Build fingers table
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = n.key.add(1L << i);
            try {
                FindSuccessorMessage m = new FindSuccessorMessage(k);
                Chord.NodeInfo s = m.sendTo(chord, g);
                if(Chord.distance(k, n.key) < Chord.distance(k, s.key)) s = n;
                chord.setFinger(i, s);
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }

        if(this.chord.exists(n.key)) {
            throw new KeyAlreadyExistsException(n.key);
        }

        // Update other nodes
        // Update predecessor of successor
        SetPredecessorProtocol setPredecessorProtocol = new SetPredecessorProtocol(chord, chord.getNodeInfo());
        setPredecessorProtocol.invoke();
        // Update other nodes' fingers tables
        FingersAddProtocol fingersAddProtocol = new FingersAddProtocol(chord);
        fingersAddProtocol.invoke();
        // Update other nodes' successor lists
        Chord.Key k = n.key;
        NotifySuccessorMessage notifySuccessorMessage = new NotifySuccessorMessage(n);
        for(int i = 0; i < Chord.SUCCESSOR_LIST_SIZE; ++i) {
            Chord.NodeInfo p = chord.findPredecessor(k);
            if(p.equals(n)) break;
            try {
                if(!notifySuccessorMessage.sendTo(chord, p.address)) return false;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            k = p.key;
        }

        // Move keys
        moveKeys.invoke();

        System.out.println("Node " + chord.getNodeInfo().key + ": Done joining");

        return true;
    }
}
