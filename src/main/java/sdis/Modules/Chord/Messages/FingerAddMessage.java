package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerAddMessage extends ChordMessage<Boolean> {

    private final Chord.NodeInfo nodeInfo;
    private final int fingerIndex;

    public FingerAddMessage(Chord.NodeInfo nodeInfo, int fingerIdx){
        this.nodeInfo = nodeInfo;
        this.fingerIndex = fingerIdx;
    }

    public FingerAddMessage(Chord chord, byte[] data){
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
        fingerIndex = Integer.parseInt(splitString[3]);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("FINGERADD " + nodeInfo + " " + fingerIndex).getBytes());
    }

    private static class FingerAddProcessor extends ChordMessage.Processor {
        final FingerAddMessage message;

        public FingerAddProcessor(Chord chord, Socket socket, FingerAddMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            try {
                Chord chord = getChord();
                Chord.NodeInfo n = chord.getNodeInfo();
                Chord.NodeInfo s = message.nodeInfo;

                // If the new node to update the fingers table is itself, ignore
                if(n.equals(s)){
                    getSocket().getOutputStream().write(message.formatResponse(true));
                    readAllBytesAndClose(getSocket());
                    return;
                }

                // Update fingers if necessary
                int i = message.fingerIndex;
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    Chord.distance(n.key.add(1L << i), s.key) < Chord.distance(n.key.add(1L << i), chord.getFingerInfo(i).key)
                ){
                    updatedFingers = true;
                    chord.setFinger(i--, s);
                }

                Chord.NodeConn p = chord.getPredecessor();
                boolean ret = true;
                // If at least one finger was updated, and the predecessor was
                // not the one that sent the message, redirect to predecessor.
                // (this is already prevented by the `r.equals(f)` check on
                // arrival, but we can also check that on departure)
                if(updatedFingers && !p.nodeInfo.equals(s)){
                    ret &= message.sendTo(chord, p.socket);
                } else {
                    try { new HelloMessage().sendTo(chord, p.socket); } catch (IOException | InterruptedException e) { e.printStackTrace(); ret = false; }
                }

                getSocket().getOutputStream().write(message.formatResponse(ret));
                readAllBytesAndClose(getSocket());
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FingerAddProcessor getProcessor(Peer peer, Socket socket) {
        return new FingerAddProcessor(peer.getChord(), socket, this);
    }

    @Override
    protected byte[] formatResponse(Boolean b) {
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    @Override
    protected Boolean parseResponse(Chord chord, byte[] data) {
        return (data.length == 1 && data[0] == 1);
    }
}
