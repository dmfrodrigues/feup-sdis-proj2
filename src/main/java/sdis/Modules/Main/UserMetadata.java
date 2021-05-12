package sdis.Modules.Main;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserMetadata implements Serializable {
    public class UserFileMetadata implements Serializable {
        private final String path;
        private final long numberOfChunks;
        private final int replicationDegree;

        public UserFileMetadata(String path, long numberOfChunks, int replicationDegree){
            this.path = path;
            this.numberOfChunks = numberOfChunks;
            this.replicationDegree = replicationDegree;
        }
    }

    private Username username;
    private Password password;
    private Map<String, UserFileMetadata> files = new HashMap<>();

    public UserMetadata(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public Password getPassword() {
        return password;
    }

    public UserFileMetadata addFile(String path, long numberOfChunks, int replicationDegree){
        UserFileMetadata file = new UserFileMetadata(path, numberOfChunks, replicationDegree);
        files.put(path, file);
        return file;
    }

    public UserFileMetadata getFile(String path){
        return files.get(path);
    }

    public void removeFile(String path){
        files.remove(path);
    }
}
