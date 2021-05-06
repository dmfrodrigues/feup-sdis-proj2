package sdis.Protocols.SystemStorage;

import sdis.Chord;
import sdis.Peer;
import sdis.PeerInfo;
import sdis.Protocols.Chord.FingersAddProtocol;
import sdis.Protocols.Chord.GetPredecessorProtocol;
import sdis.Protocols.Chord.Messages.GetSuccessorMessage;
import sdis.Protocols.Chord.UpdatePredecessorProtocol;
import sdis.Protocols.ProtocolSupplier;

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
        long r = chord.getKey();

        // Initialize fingers table and predecessor
        // Build fingers table
        for(int i = 0; i < chord.getKeySize(); ++i){
            long k = r + (1L << i);
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
