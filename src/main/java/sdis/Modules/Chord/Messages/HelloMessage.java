package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class HelloMessage extends ChordMessage<Boolean> {

    @Override
    protected DataBuilder build() {
        return new DataBuilder("HELLO".getBytes());
    }

    private static class HelloProcessor extends ChordMessage.Processor {

        private final HelloMessage message;

        public HelloProcessor(Chord chord, Socket socket, HelloMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                getSocket().getOutputStream().write(message.formatResponse(true));
                readAllBytesAndClose(getSocket());
            } catch (InterruptedException | IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public HelloMessage.HelloProcessor getProcessor(Peer peer, Socket socket) {
        return new HelloMessage.HelloProcessor(peer.getChord(), socket, this);
    }

    @Override
    public byte[] formatResponse(Boolean b){
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    @Override
    public Boolean parseResponse(Chord chord, byte[] response) {
        return (response.length == 1 && response[0] == 1);
    }
}
