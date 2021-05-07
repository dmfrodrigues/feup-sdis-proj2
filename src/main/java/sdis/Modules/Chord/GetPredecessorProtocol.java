package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetPredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class GetPredecessorProtocol extends ProtocolSupplier<Chord.NodeInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetPredecessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public Chord.NodeInfo get() {
        GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(chord, key);
        Chord.NodeInfo s = getSuccessorProtocol.get();

        try {
            Socket socket = chord.send(s, new GetPredecessorMessage());
            socket.shutdownOutput();

            byte[] response = socket.getInputStream().readAllBytes();
            return new Chord.NodeInfo(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
