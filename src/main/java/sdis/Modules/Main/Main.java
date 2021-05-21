package sdis.Modules.Main;

import sdis.Modules.SystemStorage.SystemStorage;

import java.io.Serializable;

public class Main {
    public static final int CHUNK_SIZE = 64000;

    private final SystemStorage systemStorage;

    public Main(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    public SystemStorage getSystemStorage(){
        return systemStorage;
    }

    public static class Path {
        String s;

        public Path(String s){
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }

        @Override
        public boolean equals(Object obj) {
            return s.equals(obj);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }
    }

    public static class File implements Serializable {
        public class ID {
            String s;
            private ID(String s){
                this.s = s;
            }

            @Override
            public String toString(){
                return s;
            }
        }

        private final Main.Path path;
        private final long numberOfChunks;
        private final int replicationDegree;

        public File(Main.Path path, long numberOfChunks, int replicationDegree){
            this.path = path;
            this.numberOfChunks = numberOfChunks;
            this.replicationDegree = replicationDegree;
        }

        public Main.Path getPath() {
            return path;
        }

        public long getNumberOfChunks() {
            return numberOfChunks;
        }

        public int getReplicationDegree() {
            return replicationDegree;
        }

        public File.ID getId(){
            return new File.ID("f/" + path);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(obj == null) return false;
            if(getClass() != obj.getClass()) return false;
            Main.File file = (Main.File) obj;
            return
                path.equals(file.path) &&
                numberOfChunks == file.numberOfChunks &&
                replicationDegree == file.replicationDegree
            ;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }
}
