package sdis;

/*
import sdis.Protocols.DataStorage.ReclaimProtocol;
import sdis.Protocols.Main.BackupFileProtocol;
import sdis.Protocols.Main.DeleteFileProtocol;
import sdis.Protocols.Main.RestoreFileProtocol;
*/
import sdis.Protocols.Chord.Chord;
import sdis.Protocols.DataStorage.LocalDatapieceStorageManager;
import sdis.Protocols.DataStorage.GlobalDatapieceStorageManager;

import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class Peer implements PeerInterface {
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final long id;

    private final InetSocketAddress address;
    private final ServerSocket serverSocket;

    private final GlobalDatapieceStorageManager globalDatapieceStorageManager;
    private final LocalDatapieceStorageManager storageManager;

    private final Random random = new Random(System.currentTimeMillis());

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);
    private final Chord chord;

    public Peer(
        long id,
        InetSocketAddress address
    ) throws IOException {
        // Store arguments
        this.id = id;
        this.address = address;
        System.out.println(
            "Starting peer " + this.id +
            " with address " + this.address.getAddress().getHostAddress() + ":" + this.address.getPort()
        );

        serverSocket = new ServerSocket(address.getPort());

        // Initialize storage space
        String storagePath = id + "/storage/data";
        storageManager = new LocalDatapieceStorageManager(this, storagePath, INITIAL_STORAGE_SIZE);

        globalDatapieceStorageManager = new GlobalDatapieceStorageManager("../build/"+id);
        globalDatapieceStorageManager.load();

        chord = new Chord(this, 62, id);
    }

    public static class CleanupRemoteObjectRunnable implements Runnable {
        private final String remoteObjName;

        public CleanupRemoteObjectRunnable(String remoteObjName) {
            this.remoteObjName = remoteObjName;
        }

        @Override
        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry();
                registry.unbind(remoteObjName);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
    public void bindAsRemoteObject(String remoteObjName) throws RemoteException, AlreadyBoundException {
        PeerInterface stub = (PeerInterface) UnicastRemoteObject.exportObject(this, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(remoteObjName, stub);

        CleanupRemoteObjectRunnable rmiCleanupRunnable = new CleanupRemoteObjectRunnable(remoteObjName);
        Thread rmiCleanupThread = new Thread(rmiCleanupRunnable);
        Runtime.getRuntime().addShutdownHook(rmiCleanupThread);
    }

    public Chord getChord() {
        return chord;
    }

    public Random getRandom() {
        return random;
    }

    public static ScheduledExecutorService getExecutor(){
        return executor;
    }

    public long getId() {
        return id;
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    public LocalDatapieceStorageManager getStorageManager() {
        return storageManager;
    }

    public GlobalDatapieceStorageManager getFileTable() {
        return globalDatapieceStorageManager;
    }

    /**
     * Backup file specified by pathname, with a certain replication degree.
     *
     * @param pathname          Pathname of file to be backed up
     * @param replicationDegree Replication degree (number of copies of each file chunk over all machines in the network)
     */
    public void backup(String pathname, int replicationDegree) throws IOException {
        /*
        File file = new File(pathname);
        FileChunkIterator fileChunkIterator;
        try {
            fileChunkIterator = new FileChunkIterator(file);
        } catch (NoSuchFileException e) {
            System.err.println("File " + pathname + " not found");
            return;
        }
        getFileTable().insert(file.getName(), fileChunkIterator.getFileId(), fileChunkIterator.length());
        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(this, fileChunkIterator, replicationDegree);
        CompletableFuture.runAsync(backupFileProtocol);
         */
    }

    /**
     * Restore file specified by pathname.
     *
     * That file's chunks are retrieved from peers, assembled and then saved to the provided pathname.
     *
     * @param pathname  Pathname of file to be restored
     */
    public void restore(String pathname) throws IOException {
        /*
        RestoreFileProtocol callable = new RestoreFileProtocol(this, pathname);
        executor.submit(callable);
         */
    }

    /**
     * Delete file specified by pathname.
     *
     * @param pathname  Pathname of file to be deleted over all peers
     */
    public void delete(String pathname) {
        /*
        DeleteFileProtocol callable = new DeleteFileProtocol(this, pathname);
        executor.submit(callable);
         */
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_kb  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kb) {
        /*
        ReclaimProtocol callable = new ReclaimProtocol(this, space_kb);
        executor.submit(callable);
         */
    }
}
