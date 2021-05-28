package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerRemoveMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RecursiveTask;

public class FingersRemoveProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;

    public FingersRemoveProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Boolean compute() {
        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i);
            int finalI = i;
            ProtocolTask<Boolean> task = new ProtocolTask<>() {
                @Override
                protected Boolean compute() {
                    GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(chord, k);
                    Chord.NodeInfo predecessor = getPredecessorProtocol.compute();
                    try {
                        FingerRemoveMessage m = new FingerRemoveMessage(chord.getNodeInfo(), chord.getSuccessor(), finalI);
                        Socket socket = chord.send(predecessor, m);
                        readAllBytesAndClose(socket);
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
