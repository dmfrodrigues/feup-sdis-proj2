package sdis.Modules.Chord;

import sdis.PeerInfo;
import sdis.Modules.Chord.Messages.GetPredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class GetPredecessorProtocol extends ProtocolSupplier<PeerInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetPredecessorProtocol(Chord chord, Chord.Key key){
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
