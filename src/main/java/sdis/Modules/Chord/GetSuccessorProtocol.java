package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.GetSuccessorMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Utils.Utils;

import java.io.IOException;
import java.net.Socket;

public class GetSuccessorProtocol extends ProtocolSupplier<Chord.NodeInfo> {

    private final Chord chord;
    private final Chord.Key key;

    public GetSuccessorProtocol(Chord chord, Chord.Key key){
        this.chord = chord;
        this.key = key;
    }

    @Override
    public Chord.NodeInfo get() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo p = chord.getPredecessor();

        // If r is the only node in the system
        if(p.equals(r))
            return r;

        long d1 = Chord.distance(  key, r.key); // Distance from key         to current node
        long d2 = Chord.distance(p.key, r.key); // Distance from predecessor to current node
        if(d1 < d2) {
            return r;
        }

        long d = Chord.distance(r.key, key);
        int i = (d == 0 ? 0 : Utils.log2(d));


        Chord.NodeInfo r_ = chord.getFinger(i);

        try {
            Chord.NodeInfo ret;
            if(r_.equals(r)){
                return r;
            } else {
                GetSuccessorMessage m = new GetSuccessorMessage(key);
                Socket socket = chord.send(r_, m);
                socket.shutdownOutput();
                byte[] response = socket.getInputStream().readAllBytes();
                socket.close();
                ret = m.parseResponse(chord, response);
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
