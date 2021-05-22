package sdis.Modules.Main;

import sdis.Storage.ByteArrayChunkIterator;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserMetadata implements Serializable {
    private final Username username;
    private final Password password;
    private final Map<Main.Path, Main.File> files = new HashMap<>();

    public UserMetadata(Username username, Password password){
        this.username = username;
        this.password = password;
    }

    public Username getUsername() {
        return username;
    }

    public Password getPassword() {
        return password;
    }

    public Main.File addFile(Main.Path path, long numberOfChunks, int replicationDegree){
        Main.File file = new Main.File(username, path, numberOfChunks, replicationDegree);
        return addFile(file);
    }

    public Main.File addFile(Main.File file){
        files.put(file.getPath(), file);
        return file;
    }

    public Set<Main.Path> getFiles(){
        return files.keySet();
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

    public static UserMetadata deserialize(byte[] response) throws IOException, ClassNotFoundException {
        return deserialize(response, 0, response.length);
    }

    public static UserMetadata deserialize(byte[] response, int offset, int length) throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(response, offset, length);
        ObjectInputStream ois = new ObjectInputStream(is);
        UserMetadata ret = (UserMetadata) ois.readObject();
        ois.close();
        is.close();
        return ret;
    }

    public Main.File asFile() throws IOException {
        byte[] data = serialize();
        ByteArrayChunkIterator chunkIterator = new ByteArrayChunkIterator(data, Main.CHUNK_SIZE);
        return new Main.File(username, new Main.Path(username.toString()), chunkIterator.length(), Main.USER_METADATA_REPDEG);
    }
}
