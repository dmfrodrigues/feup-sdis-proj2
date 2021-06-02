package sdis.Modules.Chord.Exceptions;

import sdis.Modules.Chord.Chord;

public class KeyAlreadyExistsException extends JoinException {
    Chord.Key key;

    public KeyAlreadyExistsException(Chord.Key key){
        super(key.toString());
        this.key = key;
    }

    public Chord.Key getKey(){
        return key;
    }
}
