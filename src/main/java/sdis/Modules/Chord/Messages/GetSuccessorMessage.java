package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.GetSuccessorProtocol;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetSuccessorMessage extends ChordMessage {

    private final Chord.Key key;

    public GetSuccessorMessage(Chord.Key key){
        this.key = key;
    }

    private Chord.Key getKey() {
        return key;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("GETSUCCESSOR " + getKey()).getBytes());
    }

    private static class GetSuccessorProcessor extends ChordMessage.Processor {

        private final GetSuccessorMessage message;

        public GetSuccessorProcessor(Chord chord, Socket socket, GetSuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            GetSuccessorProtocol protocol = new GetSuccessorProtocol(getChord(), message.getKey());
            Chord.NodeInfo nodeInfo = protocol.get();
            try {
                getSocket().getOutputStream().write(nodeInfo.toString().getBytes());
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            return null;
        }
    }

    @Override
    public GetSuccessorProcessor getProcessor(Peer peer, Socket socket) {
        return new GetSuccessorProcessor(peer.getChord(), socket, this);
    }
}
