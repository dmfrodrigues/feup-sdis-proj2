package sdis;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.Password;
import sdis.Modules.Main.Username;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    boolean backup(Username username, Password password, Main.Path path, int replicationDegree, String origin) throws RemoteException;
    boolean restore(Username username, Password password, Main.Path path, String dest) throws RemoteException;
    boolean delete(Username username, Password password, Main.Path path) throws RemoteException;
    boolean deleteAccount(Username username, Password password) throws RemoteException;
    boolean reclaim(int space_bytes) throws RemoteException;
    boolean leave() throws RemoteException;
}
