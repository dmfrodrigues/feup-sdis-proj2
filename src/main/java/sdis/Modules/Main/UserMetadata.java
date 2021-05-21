package sdis.Modules.Main;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserMetadata implements Serializable {
    private final Username username;
    private final Password password;
    private final Map<Main.Path, Main.File> files = new HashMap<>();

    public UserMetadata(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public Password getPassword() {
        return password;
    }

    public Main.File addFile(Main.Path path, long numberOfChunks, int replicationDegree){
        Main.File file = new Main.File(path, numberOfChunks, replicationDegree);
        files.put(path, file);
        return file;
    }

    public Main.File getFile(Main.Path path){
        return files.get(path);
    }

    public void removeFile(Main.Path path){
        files.remove(path);
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
        oos.close();
        os.close();
        return os.toByteArray();
    }

    public static UserMetadata deserialize(byte[] response, int offset, int length) throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(response, offset, length);
        ObjectInputStream ois = new ObjectInputStream(is);
        UserMetadata ret = (UserMetadata) ois.readObject();
        ois.close();
        is.close();
        return ret;
    }
}
