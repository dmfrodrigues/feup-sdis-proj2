package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetPredecessorMessage;
import sdis.Modules.Chord.Messages.SuccessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;

public class GetPredecessorProtocol extends ProtocolTask<Chord.NodeInfo> {

    private final Chord chord;
    private final Chord.Key id;

    public GetPredecessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.id = key;
    }

    @Override
    public Chord.NodeInfo compute() {
        SuccessorMessage successorMessage = new SuccessorMessage();
        GetPredecessorMessage getPredecessorMessage = new GetPredecessorMessage(id);

        try {
            Chord.NodeInfo n_ = chord.getNodeInfo();
            Chord.NodeInfo nSuccessor = successorMessage.sendTo(chord, n_.address);
            while (!id.inRange(n_.key.add(1), nSuccessor.key)){
                n_ = getPredecessorMessage.sendTo(chord, n_.address);
            }
            return n_;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
