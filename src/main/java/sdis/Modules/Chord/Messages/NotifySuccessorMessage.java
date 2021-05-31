package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class NotifySuccessorMessage extends ChordMessage<Boolean> {

    private final Chord.NodeInfo nodeInfo;

    public NotifySuccessorMessage(Chord.NodeInfo nodeInfo){
        this.nodeInfo = nodeInfo;
    }

    public NotifySuccessorMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        String[] splitAddress = splitString[2].split(":");
        nodeInfo = new Chord.NodeInfo(
            chord.newKey(Long.parseLong(splitString[1])),
            new InetSocketAddress(
                    splitAddress[0],
                    Integer.parseInt(splitAddress[1])
            )
        );
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("NTFYSUCCESSOR " + nodeInfo).getBytes());
    }

    private static class NotifySuccessorProcessor extends Processor {

        private final NotifySuccessorMessage message;

        public NotifySuccessorProcessor(Chord chord, Socket socket, NotifySuccessorMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            getChord().addSuccessor(message.nodeInfo);
            try {
                getSocket().getOutputStream().write(message.formatResponse(true));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public NotifySuccessorProcessor getProcessor(Peer peer, Socket socket) {
        return new NotifySuccessorProcessor(peer.getChord(), socket, this);
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
