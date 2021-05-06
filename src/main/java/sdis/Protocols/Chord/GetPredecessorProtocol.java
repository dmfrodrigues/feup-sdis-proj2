package sdis.Protocols.Chord;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.Chord.Messages.GetPredecessorMessage;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class GetPredecessorProtocol extends ProtocolSupplier<PeerInfo> {

    private final Chord chord;
    private final long key;

    public GetPredecessorProtocol(Chord chord, long key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public PeerInfo get() {
        GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(chord, key);
        PeerInfo s = getSuccessorProtocol.get();

        try {
            Socket socket = chord.send(s, new GetPredecessorMessage());
            socket.shutdownOutput();

            byte[] response = socket.getInputStream().readAllBytes();
            return new PeerInfo(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
