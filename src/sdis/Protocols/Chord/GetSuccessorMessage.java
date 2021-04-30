package sdis.Protocols.Chord;

public class GetSuccessorMessage extends ChordMessage {

    private long key;

    public GetSuccessorMessage(long key){
        this.key = key;
    }

    @Override
    public String toString() {
        return "GETSUCCESSOR " + key;
    }
}
