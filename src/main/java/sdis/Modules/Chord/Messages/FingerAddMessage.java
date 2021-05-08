package sdis.Modules.Chord.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class FingerAddMessage extends ChordMessage {

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

    public Chord.NodeInfo getPeerInfo(){
        return nodeInfo;
    }

    public int getFingerIndex(){
        return fingerIndex;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("FINGERADD " + nodeInfo + " " + getFingerIndex()).getBytes());
    }

    private static class FingerAddProcessor extends ChordMessage.Processor {
        final FingerAddMessage message;

        public FingerAddProcessor(Chord chord, Socket socket, FingerAddMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            try {
                Chord chord = getChord();
                // System.out.println("        Peer " + chord.getKey() + " is processing FINGERADD");
                Chord.NodeInfo s = chord.getNodeInfo();
                Chord.NodeInfo r = message.getPeerInfo();
                // Update fingers if necessary
                int i = message.getFingerIndex();
                boolean updatedFingers = false;
                while(
                    i >= 0 &&
                    Chord.distance(s.key.add(1L << i), r.key) < Chord.distance(s.key.add(1L << i), chord.getFinger(i).key)
                ){
                    updatedFingers = true;
                    chord.setFinger(i--, r);
                }
                // If at least one finger was updated, redirect to predecessor
                if(updatedFingers){
                    // System.out.println("        Peer " + chord.getKey() + " updated fingers");
                    chord.send(chord.getPredecessor(), message);
                } else {
                    // System.out.println("        Peer " + chord.getKey() + " did not update any fingers");
                }
                getSocket().shutdownOutput();
                getSocket().getInputStream().readAllBytes();
                getSocket().close();

                // System.out.println("        Peer " + chord.getKey() + " is done with FINGERADD");

                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public FingerAddProcessor getProcessor(Peer peer, Socket socket) {
        return new FingerAddProcessor(peer.getChord(), socket, this);
    }
}
