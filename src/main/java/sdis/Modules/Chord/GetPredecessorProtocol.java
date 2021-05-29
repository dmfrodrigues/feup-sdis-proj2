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

    private static Chord.NodeInfo getClosestPrecedingFinger(Chord chord, Chord.Key id) {
        Chord.NodeInfo n = chord.getNodeInfo();
        for(int i = chord.getKeySize()-1; i >= 0; --i){
            Chord.NodeInfo finger = chord.getFinger(i);
            if(finger.key.inRange(n.key.add(1), id.subtract(1))){
                return finger;
            }
        }
        return null;
    }

    @Override
    public Chord.NodeInfo compute() {
        SuccessorMessage successorMessage = new SuccessorMessage();

        try {
            Chord.NodeInfo n = chord.getNodeInfo();
            Chord.NodeInfo nSuccessor = successorMessage.sendTo(chord, n.address);
            if (!id.inRange(n.key.add(1), nSuccessor.key)){
                Chord.NodeInfo closestPrecedingFinger = getClosestPrecedingFinger(chord, id);
                GetPredecessorMessage getPredecessorMessage = new GetPredecessorMessage(id);
                return getPredecessorMessage.sendTo(chord, closestPrecedingFinger.address);
            }
            return n;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
