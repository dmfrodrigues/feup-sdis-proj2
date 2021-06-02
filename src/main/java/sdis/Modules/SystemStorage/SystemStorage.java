package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;

public class SystemStorage {
    private final Chord chord;
    private final DataStorage dataStorage;

    public SystemStorage(Chord chord, DataStorage dataStorage){
        this.chord = chord;
        this.dataStorage = dataStorage;
    }

    public Chord getChord() {
        return chord;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
    
    public Socket sendAny(Message m) throws IOException {
        Chord.NodeConn to = chord.getSuccessor();
        to.socket.getOutputStream().write(m.asByteArray());
        to.socket.getOutputStream().flush();
        return to.socket;
    }

    public boolean put(UUID id, byte[] data) {
        return new PutSystemProtocol(this, id, data).invoke();
    }

    public boolean head(UUID id) {
        return new HeadSystemProtocol(this, id).invoke();
    }

    public byte[] get(UUID id) {
        return new GetSystemProtocol(this, id).invoke();
    }

    public boolean delete(UUID id) {
        return new DeleteSystemProtocol(this, id).invoke();
    }
}
