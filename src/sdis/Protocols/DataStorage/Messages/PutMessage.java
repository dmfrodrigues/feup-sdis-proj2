package sdis.Protocols.DataStorage.Messages;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.Chord.GetSuccessorProtocol;
import sdis.Protocols.DataStorage.PutProtocol;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class PutMessage extends DataSystemMessage {

    private long nodeKey;
    private long dataKey;
    private byte[] data;

    public PutMessage(long nodeKey, long dataKey, byte[] data){
        this.nodeKey = nodeKey;
        this.dataKey = dataKey;
        this.data = data;
    }

    private long getNodeKey() {
        return nodeKey;
    }

    private long getDataKey() {
        return dataKey;
    }

    private byte[] getData(){
        return data;
    }

    @Override
    public String toString() {
        return "PUT " + getNodeKey() + " " + getDataKey() + "\n" + new String(getData());
    }

    private static class PutProcessor extends Processor {

        private final PutMessage message;

        public PutProcessor(Chord chord, Socket socket, PutMessage message){
            super(chord, socket);
            this.message = message;
        }

        @Override
        public Void get() {
            PutProtocol putProtocol = new PutProtocol(getChord(), message.getNodeKey(), message.getDataKey(), message.getData());
            putProtocol.get();
            return null;
        }
    }

    @Override
    public PutProcessor getProcessor(Chord chord, Socket socket) {
        return new PutProcessor(chord, socket, this);
    }
}
