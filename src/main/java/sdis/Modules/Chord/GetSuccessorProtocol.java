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
        Chord.NodeInfo r = chord.getPeerInfo();
        Chord.NodeInfo p = chord.getPredecessor();

        // If r is the only node in the system
        if(p.equals(r))
            return r;

        if(
            chord.distance(p.key, key) <=
            chord.distance(p.key, r.key)
        )
            return r;

        System.out.println("L27");

        long d = chord.distance(chord.getKey(), key);
        int i = (d == 0 ? 0 : Utils.log2(d));


        Chord.NodeInfo r_ = chord.getFinger(i);

        try {
            Chord.NodeInfo ret;
            if(r_.equals(r)){
                GetSuccessorProtocol newGetSuccessorProtocol = new GetSuccessorProtocol(chord, key);
                ret = newGetSuccessorProtocol.get();
            } else {
                Socket socket = chord.send(r_, new GetSuccessorMessage(key));
                socket.shutdownOutput();

                byte[] response = socket.getInputStream().readAllBytes();
                ret = new Chord.NodeInfo(response);
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
