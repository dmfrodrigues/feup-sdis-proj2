package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.SuccessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;

public class GetSuccessorProtocol extends ProtocolTask<Chord.NodeInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetSuccessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public Chord.NodeInfo compute() {
        Chord.NodeInfo p = chord.getPredecessor(key);
        SuccessorMessage successorMessage = new SuccessorMessage();
        try {
            return successorMessage.sendTo(chord, p.address);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
