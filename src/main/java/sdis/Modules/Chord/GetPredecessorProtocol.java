package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetPredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class GetPredecessorProtocol extends ProtocolSupplier<Chord.NodeInfo> {

    private final Chord chord;
    private Chord.Key key;
    private InetSocketAddress address;

    public GetPredecessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    public GetPredecessorProtocol(Chord chord, InetSocketAddress address){
        this.chord = chord;
        this.address = address;
    }

    @Override
    public Chord.NodeInfo get() {
        if(address == null) {
            GetSuccessorProtocol getSuccessorProtocol = new GetSuccessorProtocol(chord, key);
            Chord.NodeInfo s = getSuccessorProtocol.get();
            address = s.address;
        }

        // If we are searching for the predecessor of the current node
        if(address.equals(chord.getSocketAddress())){
            return chord.getPredecessor();
        }

        try {
            Socket socket = chord.send(address, new GetPredecessorMessage());
            socket.shutdownOutput();

            byte[] response = socket.getInputStream().readAllBytes();
            return chord.newNodeInfo(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
