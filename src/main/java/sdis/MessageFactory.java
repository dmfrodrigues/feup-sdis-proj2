package sdis;

import sdis.Modules.Chord.Messages.*;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.Message;
import sdis.Utils.Utils;

public class MessageFactory {
    private static final int MAX_HEADER_SIZE = 32;

    public MessageFactory(){}

    public Message factoryMethod(byte[] b) throws ClassNotFoundException {
        int i = Utils.find_nth(b, " ".getBytes(), 1);
        byte[] start = new byte[i];
        System.arraycopy(b, 0, start, 0, i);
        String startStr = new String(start);
        switch(startStr){
            case "FINGERADD"        : return new FingerAddMessage        (b);
            case "FINGERREMOVE"     : return new FingerRemoveMessage     (b);
            case "GETPREDECESSOR"   : return new GetPredecessorMessage   ();
            case "GETSUCCESSOR"     : return new GetSuccessorMessage     (b);
            case "UPDATEPREDECESSOR": return new UpdatePredecessorMessage(b);

            case "DELETE": return new DeleteMessage(b);
            case "GET"   : return new GetMessage   (b);
            case "PUT"   : return new PutMessage   (b);
        }

        throw new ClassNotFoundException(startStr);
    }
}
