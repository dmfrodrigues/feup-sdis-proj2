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

    public void addFile(Main.File file){
        files.put(file.getPath(), file);
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
        return new Main.UserMetadataFile(username, chunkIterator.length());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        UserMetadata userMetadata = (UserMetadata) obj;
        return
            username.equals(userMetadata.username) &&
            password.equals(userMetadata.password) &&
            files.equals(userMetadata.files)
        ;
    }
}
