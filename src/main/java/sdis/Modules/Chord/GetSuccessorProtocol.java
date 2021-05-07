package sdis.Modules.Chord;

import sdis.Modules.Chord.Chord;
import sdis.PeerInfo;
import sdis.Modules.Chord.Messages.GetSuccessorMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.Socket;

public class GetSuccessorProtocol extends ProtocolSupplier<PeerInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetSuccessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public PeerInfo get() {
        if(chord.getPredecessor().key.compareTo(key) < 0 && key.compareTo(chord.getKey()) <= 0)
            return chord.getPeerInfo();

        long d = chord.distance(chord.getKey(), key);
        int i = Utils.log2(d);
        PeerInfo r_ = chord.getFinger(i);

        try {
            Socket socket = chord.send(r_, new GetSuccessorMessage(key));
            socket.shutdownOutput();

            byte[] response = socket.getInputStream().readAllBytes();
            return new PeerInfo(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
