package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.PutSystemProtocol;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;
import sdis.UUID;
import sdis.Utils.DataBuilder;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class MoveKeysMessage extends SystemStorageMessage {

    private final Chord.NodeInfo nodeInfo;

    public MoveKeysMessage(Chord.NodeInfo nodeInfo){
        this.nodeInfo = nodeInfo;
    }

    public MoveKeysMessage(Chord chord, byte[] data){
        String dataString = new String(data);
        String[] splitString = dataString.split(" ");
        Chord.Key key = chord.newKey(Long.parseLong(splitString[0]));
        String[] splitAddress = splitString[1].split(":");
        InetSocketAddress address = new InetSocketAddress(splitAddress[0], Integer.parseInt(splitAddress[1]));
        nodeInfo = new Chord.NodeInfo(key, address);
    }

    private Chord.NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    protected DataBuilder build() {
        return new DataBuilder(("MOVEKEYS " + getNodeInfo()).getBytes());
    }

    private static class MoveKeysProcessor extends Processor {

        private final MoveKeysMessage message;

        public MoveKeysProcessor(SystemStorage systemStorage, Socket socket, MoveKeysMessage message){
            super(systemStorage, socket);
            this.message = message;
        }

        @Override
        public void compute() {
            SystemStorage systemStorage = getSystemStorage();
            DataStorage dataStorage = systemStorage.getDataStorage();
            Chord chord = systemStorage.getChord();
            Chord.NodeInfo r = chord.getNodeInfo();
            Chord.NodeInfo n = message.getNodeInfo();

            Set<UUID> ids = dataStorage.getAll();
            List<RecursiveTask<Boolean>> tasks = new LinkedList<>();
            for(UUID id: ids){
                Chord.Key k = id.getKey(chord);
                if(Chord.distance(k, n.key) < Chord.distance(k, r.key)){
                    RecursiveTask<Boolean> task = new ProtocolTask<>() {
                        @Override
                        protected Boolean compute() {
                            byte[] data = dataStorage.get(id);
                            dataStorage.delete(id);
                            PutSystemProtocol putSystemProtocol = new PutSystemProtocol(systemStorage, id, data);
                            return putSystemProtocol.invoke();
                        }
                    };
                    task.fork();
                    tasks.add(task);
                }
            }

            for(RecursiveTask<Boolean> task: tasks) task.join();
        }
    }

    @Override
    public MoveKeysProcessor getProcessor(Peer peer, Socket socket) {
        return new MoveKeysProcessor(peer.getSystemStorage(), socket, this);
    }
}
