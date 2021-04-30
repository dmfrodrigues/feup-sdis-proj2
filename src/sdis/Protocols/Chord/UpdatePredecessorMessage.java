package sdis.Protocols.Chord;

import sdis.PeerInfo;

public class UpdatePredecessorMessage extends ChordMessage {

    private PeerInfo predecessor;

    public UpdatePredecessorMessage(PeerInfo predecessor){
        this.predecessor = predecessor;
    }

    @Override
    public String toString() {
        return "UPPREDECESSOR " + predecessor.toString();
    }
}
