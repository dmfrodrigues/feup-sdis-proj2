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
        // System.out.println("Peer " + chord.getKey() + " starting GetSuccessor protocol");

        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo p = chord.getPredecessor();

        // If r is the only node in the system
        if(p.equals(r))
            return r;

        // System.out.println("    Peer " + chord.getKey() + " is not alone");

        long d1 = Chord.distance(  key, r.key); // Distance from key         to current node
        long d2 = Chord.distance(p.key, r.key); // Distance from predecessor to current node
        if(d1 < d2) {
            // System.out.println("    Peer " + chord.getKey() + " believes it is the owner of key " + key);
            // System.out.println("    p=" + p + "    r=" + r + "    key=" + key);
            // System.out.println("    Chord.distance(p.key,   key)=" + Chord.distance(p.key,   key));
            // System.out.println("    Chord.distance(p.key, r.key)=" + Chord.distance(p.key, r.key));

            return r;
        }

        System.out.println("    Peer " + chord.getKey() + " is not the owner of key " + key);

        long d = Chord.distance(chord.getKey(), key);
        int i = (d == 0 ? 0 : Utils.log2(d));


        Chord.NodeInfo r_ = chord.getFinger(i);

        try {
            Chord.NodeInfo ret;
            if(r_.equals(r)){
                GetSuccessorProtocol newGetSuccessorProtocol = new GetSuccessorProtocol(chord, key);
                ret = newGetSuccessorProtocol.get();
            } else {
                GetSuccessorMessage m = new GetSuccessorMessage(key);
                Socket socket = chord.send(r_, m);
                socket.shutdownOutput();

                // System.out.println("    Peer " + chord.getKey() + "\t sent " + new String(m.asByteArray()) + " to " + r_);

                byte[] response = socket.getInputStream().readAllBytes();
                ret = chord.newNodeInfo(response);

                // System.out.println("    Peer " + chord.getKey() + "\t got response to " + new String(m.asByteArray()));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
