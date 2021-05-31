package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerAddMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class FingersAddProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;

    public FingersAddProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Boolean compute() {
        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getNodeInfo().key.subtract(1L << i).add(1);
            int finalI = i;
            ProtocolTask<Boolean> task = new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    Chord.NodeInfo predecessor = chord.findPredecessor(k);
                    try {
                        FingerAddMessage m = new FingerAddMessage(chord.getNodeInfo(), finalI);
                        m.sendTo(chord, predecessor.address);
                        return true;
                    } catch (IOException | InterruptedException e) {
                        throw new CompletionException(e);
                    }
                }
            };
            tasks.add(task);
        }
        return invokeAndReduceTasks(tasks);
    }
}
