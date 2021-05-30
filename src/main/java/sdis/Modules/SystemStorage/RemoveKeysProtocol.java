package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.PutSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveKeysProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;

    public RemoveKeysProtocol(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    @Override
    public Boolean compute() {
        Chord.NodeConn s = systemStorage.getChord().getSuccessor();
        DataStorage dataStorage = systemStorage.getDataStorage();

        Set<UUID> ids = dataStorage.getAll();
        List<ProtocolTask<Boolean>> tasks = ids.stream().map((UUID id) -> new ProtocolTask<Boolean>() {
            @Override
            protected Boolean compute() {
                byte[] data = dataStorage.get(id);
                if(!dataStorage.delete(id)) return false;
                PutSystemMessage putSystemMessage = new PutSystemMessage(id, data);

                try {
                    return putSystemMessage.sendTo(s.nodeInfo.address);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }).collect(Collectors.toList());

        return invokeAndReduceTasks(tasks);
    }
}
