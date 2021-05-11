package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.SystemStorage.Messages.MoveKeysMessage;
import sdis.Modules.SystemStorage.Messages.SystemStorageMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

    public Socket send(InetSocketAddress to, SystemStorageMessage m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }
}
