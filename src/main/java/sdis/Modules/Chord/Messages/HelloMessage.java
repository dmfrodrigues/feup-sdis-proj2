package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletionException;

public class HelloMessage extends ChordMessage<Boolean> {

    @Override
    protected DataBuilder build() {
        return new DataBuilder("HELLO".getBytes());
    }

    private static class HelloProcessor extends ChordMessage.Processor {

        private final HelloMessage message;

        public HelloProcessor(Chord chord, SocketChannel socket, HelloMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                getSocket().write(message.formatResponse(true));
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public HelloMessage.HelloProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new HelloMessage.HelloProcessor(peer.getChord(), socket, this);
    }

    @Override
    public ByteBuffer formatResponse(Boolean b){
        return ByteBuffer.wrap(new byte[]{(byte) (b ? 1 : 0)});
    }

    @Override
    public Boolean parseResponse(Chord chord, ByteBuffer response) {
        return (response.position() == 1 && response.array()[0] == 1);
    }
}
