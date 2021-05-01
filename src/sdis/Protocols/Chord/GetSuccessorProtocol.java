package sdis.Protocols.Chord;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.ProtocolSupplier;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.Socket;

public class GetSuccessorProtocol extends ProtocolSupplier<PeerInfo> {

    private final Chord chord;
    private final long key;

    public GetSuccessorProtocol(Chord chord, long key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public PeerInfo get() {
        if(chord.getPredecessor().key < key && key <= chord.getKey())
            return chord.asPeerInfo();

        long MOD = 1L << chord.getKeySize();
        long distance = (key - chord.getKey() + MOD) % MOD;
        int k = Utils.log2(distance);

        try {
            Socket socket = chord.send(chord.getFinger(k), new GetSuccessorMessage(key));
            socket.shutdownOutput();

            byte[] response = socket.getInputStream().readAllBytes();
            return new PeerInfo(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
