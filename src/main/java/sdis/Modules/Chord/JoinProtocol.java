package sdis.Modules.Chord;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.PeerInfo;
import sdis.Modules.Chord.FingersAddProtocol;
import sdis.Modules.Chord.GetPredecessorProtocol;
import sdis.Modules.Chord.Messages.GetSuccessorMessage;
import sdis.Modules.Chord.UpdatePredecessorProtocol;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class JoinProtocol extends ProtocolSupplier<Void> {
    private final Peer peer;
    private final InetSocketAddress g;
    private final ProtocolSupplier<Void> moveKeys;

    public JoinProtocol(Peer peer, InetSocketAddress gateway, ProtocolSupplier<Void> moveKeys){
        this.peer = peer;
        this.g = gateway;
        this.moveKeys = moveKeys;
    }

    @Override
    public Void get() {
        Chord chord = peer.getChord();
        Chord.Key r = chord.getKey();

        // Initialize fingers table and predecessor
        // Build fingers table
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = r.add(1L << i);
            try {
                Socket socket = chord.send(g, new GetSuccessorMessage(k));
                byte[] response = socket.getInputStream().readAllBytes();
                PeerInfo s = new PeerInfo(response);
                chord.setFinger(i, s);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
        // Get predecessor
        GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(chord, chord.getKey());
        PeerInfo predecessor = getPredecessorProtocol.get();
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
