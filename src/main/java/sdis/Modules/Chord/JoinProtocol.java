package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FindSuccessorMessage;
import sdis.Modules.Chord.Messages.PredecessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
        System.out.println("Peer " + chord.getKey() + " starting to join");

        Chord.NodeInfo r = chord.getNodeInfo();

        // Initialize fingers table and predecessor
        // Get predecessor
        try {
            // Get successor
            FindSuccessorMessage findSuccessorMessage = new FindSuccessorMessage(r.key);
            Chord.NodeInfo s = findSuccessorMessage.sendTo(chord, g);
            chord.setFinger(0, s);
            // Get predecessor
            PredecessorMessage predecessorMessage = new PredecessorMessage();
            chord.setPredecessor(predecessorMessage.sendTo(chord, s.address));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        // Build fingers table
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = r.key.add(1L << i);
            try {
                FindSuccessorMessage m = new FindSuccessorMessage(k);
                Chord.NodeInfo s = m.sendTo(chord, g);
                if(Chord.distance(k, r.key) < Chord.distance(k, s.key)) s = r;
                chord.setFinger(i, s);
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }

        // Update other nodes
        // Update predecessor of successor
        SetPredecessorProtocol setPredecessorProtocol = new SetPredecessorProtocol(chord, chord.getNodeInfo());
        setPredecessorProtocol.invoke();
        // Update other nodes' fingers tables
        FingersAddProtocol fingersAddProtocol = new FingersAddProtocol(chord);
        fingersAddProtocol.invoke();

        // Move keys
        moveKeys.invoke();

        System.out.println("Peer " + chord.getKey() + " done joining");

        return true;
    }
}
