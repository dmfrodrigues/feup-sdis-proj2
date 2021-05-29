package sdis;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.Messages.*;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.DataStorage.Messages.GetRedirectsMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.Main.Messages.AuthenticateMessage;
import sdis.Modules.Main.Messages.DeleteAccountMessage;
import sdis.Modules.Main.Messages.DelistFileMessage;
import sdis.Modules.Main.Messages.EnlistFileMessage;
import sdis.Modules.Message;
import sdis.Modules.SystemStorage.Messages.DeleteSystemMessage;
import sdis.Modules.SystemStorage.Messages.GetSystemMessage;
import sdis.Modules.SystemStorage.Messages.MoveKeysMessage;
import sdis.Modules.SystemStorage.Messages.PutSystemMessage;
import sdis.Utils.Utils;

public class MessageFactory {
    private final Peer peer;

    public MessageFactory(Peer peer){
        this.peer = peer;
    }

    public Message factoryMethod(byte[] b) throws ClassNotFoundException {
        Chord chord = peer.getChord();

        int i = Utils.find_nth(b, " ".getBytes(), 1);
        if(i == -1) i = b.length;
        byte[] start = new byte[i];
        System.arraycopy(b, 0, start, 0, i);
        String startStr = new String(start);
        switch(startStr){
            case "FINGERADD"     : return new FingerAddMessage     (chord, b);
            case "FINGERREMOVE"  : return new FingerRemoveMessage  (chord, b);
            case "GETSUCCESSOR"  : return new GetSuccessorMessage  (chord, b);
            case "SETPREDECESSOR": return new SetPredecessorMessage(chord, b);
            case "PREDECESSOR"   : return new PredecessorMessage();

            case "DELETE"      : return new DeleteMessage      (b);
            case "GET"         : return new GetMessage         (b);
            case "PUT"         : return new PutMessage         (chord, b);
            case "GETREDIRECTS": return new GetRedirectsMessage();

            case "MOVEKEYS"    : return new MoveKeysMessage    (chord, b);
            case "PUTSYSTEM"   : return new PutSystemMessage   (b);
            case "GETSYSTEM"   : return new GetSystemMessage   (b);
            case "DELETESYSTEM": return new DeleteSystemMessage(b);

            case "AUTHENTICATE" : return new AuthenticateMessage (b);
            case "ENLISTFILE"   : return new EnlistFileMessage   (b);
            case "DELISTFILE"   : return new DelistFileMessage   (b);
            case "DELETEACCOUNT": return new DeleteAccountMessage(b);
        }

        throw new ClassNotFoundException(startStr);
    }
}
