package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetSuccessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class JoinProtocol extends ProtocolSupplier<Void> {
    private final Chord chord;
    private final InetSocketAddress g;
    private final ProtocolSupplier<Void> moveKeys;

    public JoinProtocol(Chord chord, InetSocketAddress gateway, ProtocolSupplier<Void> moveKeys){
        this.chord = chord;
        this.g = gateway;
        this.moveKeys = moveKeys;
    }

    @Override
    public Void get() {
        System.out.println("Peer " + chord.getKey() + " starting to join");

        Chord.NodeInfo r = chord.getNodeInfo();

        // Initialize fingers table and predecessor
        // Build fingers table
        // System.out.println("Peer " + chord.getKey() + " building fingers table");
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = r.key.add(1L << i);
            try {
                Socket socket = chord.send(g, new GetSuccessorMessage(k));
                socket.shutdownOutput();
                byte[] response = socket.getInputStream().readAllBytes();
                // System.out.println("Peer " + chord.getKey() + " got answer to GETSUCCESSOR " + k);
                Chord.NodeInfo s = chord.newNodeInfo(response);
                if(chord.distance(k, r.key) < chord.distance(k, s.key)) s = r;
                chord.setFinger(i, s);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
        // Get predecessor
//        System.out.println("Peer " + chord.getKey() + " getting predecessor");
        GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(chord, chord.getSuccessor().address);
        Chord.NodeInfo predecessor = getPredecessorProtocol.get();
//        System.out.println("Peer " + chord.getKey() + " got predecessor: " + predecessor);
        chord.setPredecessor(predecessor);

        // Update other nodes
        // Update predecessor of successor
        UpdatePredecessorProtocol updatePredecessorProtocol = new UpdatePredecessorProtocol(chord, chord.getNodeInfo());
        updatePredecessorProtocol.get();
        // Update other nodes' fingers tables
        FingersAddProtocol fingersAddProtocol = new FingersAddProtocol(chord);
        fingersAddProtocol.get();

        // Move keys
        moveKeys.get();

        System.out.println("Peer " + chord.getKey() + " done joining");

        return null;
    }
}
