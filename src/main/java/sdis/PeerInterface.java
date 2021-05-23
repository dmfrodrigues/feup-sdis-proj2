package sdis;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.Password;
import sdis.Modules.Main.Username;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    void backup(Username username, Password password, Main.Path path, int replicationDegree, ChunkIterator chunkIterator);
    void restore(Username username, Password password, Main.Path path, ChunkOutput chunkOutput);
    void delete(Username username, Password password, Main.Path path);
    void reclaim(int space_bytes) throws RemoteException;
}
