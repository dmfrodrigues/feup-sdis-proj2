package sdis.Modules.Main;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserMetadata implements Serializable {
    public class File implements Serializable {
        private final String path;
        private final long numberOfChunks;
        private final int replicationDegree;

        public File(String path, long numberOfChunks, int replicationDegree){
            this.path = path;
            this.numberOfChunks = numberOfChunks;
            this.replicationDegree = replicationDegree;
        }

        public String getPath() {
            return path;
        }

        public long getNumberOfChunks() {
            return numberOfChunks;
        }

        public int getReplicationDegree() {
            return replicationDegree;
        }
    }

    private final Username username;
    private final Password password;
    private final Map<String, File> files = new HashMap<>();

    public UserMetadata(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public Password getPassword() {
        return password;
    }

    public File addFile(String path, long numberOfChunks, int replicationDegree){
        File file = new File(path, numberOfChunks, replicationDegree);
        files.put(path, file);
        return file;
    }

    public File getFile(String path){
        return files.get(path);
    }

    public void removeFile(String path){
        files.remove(path);
    }
}
