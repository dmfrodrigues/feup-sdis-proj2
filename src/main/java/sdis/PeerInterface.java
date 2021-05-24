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
    boolean backup(Username username, Password password, Main.Path path, int replicationDegree, ChunkIterator chunkIterator);
    boolean restore(Username username, Password password, Main.Path path, ChunkOutput chunkOutput);
    boolean delete(Username username, Password password, Main.Path path);
    boolean reclaim(int space_bytes) throws RemoteException;
}
