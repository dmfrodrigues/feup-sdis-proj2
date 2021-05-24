package sdis.Modules.DataStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.HelloProtocol;
import sdis.Peer;
import sdis.Utils.DataBuilder;

import java.net.InetSocketAddress;
import java.net.Socket;

public class HelloMessage extends DataStorageMessage {
    private final Chord.NodeInfo nodeInfo;

    public HelloMessage(Chord.NodeInfo nodeInfo){
        this.nodeInfo = nodeInfo;
    }

    public HelloMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        Chord.Key key = chord.newKey(Long.parseLong(splitString[0]));
        String[] splitAddress = splitString[1].split(":");
        InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        nodeInfo = new Chord.NodeInfo(key, address);
    }

    private Chord.NodeInfo getNodeInfo(){
        return nodeInfo;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("HELLO " + getNodeInfo()).getBytes());
    }

    private static class HelloProcessor extends Processor {

        private final HelloMessage message;

        public HelloProcessor(Chord chord, DataStorage dataStorage, Socket socket, HelloMessage message){
            super(chord, dataStorage, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            // If the message was received by the node that started the first Hello protocol, ignore
            if(getChord().getNodeInfo().equals(message.getNodeInfo())) return null;

            HelloProtocol helloProtocol = new HelloProtocol(getChord(), getDataStorage(), message.getNodeInfo());
            helloProtocol.get();
            return null;
        }
    }

    @Override
    public HelloProcessor getProcessor(Peer peer, Socket socket) {
        return new HelloProcessor(peer.getChord(), peer.getDataStorage(), socket, this);
    }

    private byte[] formatResponse(boolean b) {
        byte[] ret = new byte[1];
        ret[0] = (byte) (b ? 1 : 0);
        return ret;
    }

    public boolean parseResponse(byte[] response) {
        return (response[0] != 0);
    }
}
