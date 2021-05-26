package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetPredecessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class GetPredecessorProtocol extends ProtocolTask<Chord.NodeInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetPredecessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public Chord.NodeInfo compute() {
        GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(chord, key);
        Chord.NodeInfo s = getSuccessorProtocol.compute();

        // If we are searching for the predecessor of the current node
        if(s.key.equals(key)){
            return s;
        }

        try {
            GetPredecessorMessage message = new GetPredecessorMessage();
            Socket socket = chord.send(s.address, message);
            byte[] response = readAllBytesAndClose(socket);
            return message.parseResponse(chord, response);
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
