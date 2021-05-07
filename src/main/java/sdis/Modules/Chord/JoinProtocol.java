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
        Chord.Key r = chord.getKey();

        // Initialize fingers table and predecessor
        // Build fingers table
        for(int i = 0; i < Chord.getKeySize(); ++i){
            Chord.Key k = r.add(1L << i);
            try {
                Socket socket = chord.send(g, new GetSuccessorMessage(k));
                byte[] response = socket.getInputStream().readAllBytes();
                Chord.NodeInfo s = new Chord.NodeInfo(response);
                chord.setFinger(i, s);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
        // Get predecessor
        GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(chord, chord.getKey());
        Chord.NodeInfo predecessor = getPredecessorProtocol.get();
        chord.setPredecessor(predecessor);

        // Update other nodes
        // Update predecessor of successor
        UpdatePredecessorProtocol updatePredecessorProtocol = new UpdatePredecessorProtocol(chord, chord.getPeerInfo());
        updatePredecessorProtocol.get();
        // Update other nodes' fingers tables
        FingersAddProtocol fingersAddProtocol = new FingersAddProtocol(chord);
        fingersAddProtocol.get();

        // Move keys
        moveKeys.get();

        return null;
    }
}
