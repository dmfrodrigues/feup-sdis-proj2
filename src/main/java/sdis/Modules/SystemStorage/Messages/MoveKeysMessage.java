package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.PutSystemProtocol;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class MoveKeysMessage extends SystemStorageMessage<Boolean> {

    private final Chord.NodeInfo nodeInfo;

    public MoveKeysMessage(Chord.NodeInfo nodeInfo){
        this.nodeInfo = nodeInfo;
    }

    public MoveKeysMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        Chord.Key key = chord.newKey(Long.parseLong(splitString[1]));
        String[] splitAddress = splitString[2].split(":");
        InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        nodeInfo = new Chord.NodeInfo(key, address);
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("MOVEKEYS " + nodeInfo).getBytes());
    }

    private static class MoveKeysProcessor extends Processor {

        private final MoveKeysMessage message;

        public MoveKeysProcessor(SystemStorage systemStorage, SocketChannel socket, MoveKeysMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            SystemStorage systemStorage = getSystemStorage();
            DataStorage dataStorage = systemStorage.getDataStorage();
            Chord chord = systemStorage.getChord();
            Chord.NodeInfo r = chord.getNodeInfo();
            Chord.NodeInfo n = message.nodeInfo;

            Set<UUID> ids = dataStorage.getAll();
            List<ProtocolTask<Boolean>> tasks = new LinkedList<>();
            for(UUID id: ids){
                Chord.Key k = id.getKey(chord);
                if(Chord.distance(k, n.key) < Chord.distance(k, r.key)){
                    ProtocolTask<Boolean> task = new ProtocolTask<>() {
                        @Override
                        protected Boolean compute() {
                            byte[] data = dataStorage.get(id);
                            dataStorage.delete(id);
                            PutSystemProtocol putSystemProtocol = new PutSystemProtocol(systemStorage, id, data);
                            return putSystemProtocol.invoke();
                        }
                    };
                    tasks.add(task);
                }
            }

            boolean ret = ProtocolTask.invokeAndReduceTasks(tasks);

            try {
                getSocket().write(message.formatResponse(ret));
                getSocket().close();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Override
    public MoveKeysProcessor getProcessor(Peer peer, SocketChannel socket) {
        return new MoveKeysProcessor(peer.getSystemStorage(), socket, this);
    }

    @Override
    protected ByteBuffer formatResponse(Boolean b) {
        return ByteBuffer.wrap(new byte[]{(byte) (b ? 1 : 0)});
    }

    @Override
    public Boolean parseResponse(ByteBuffer response) {
        return (response.position() == 1 && response.array()[0] == 1);
    }
}
