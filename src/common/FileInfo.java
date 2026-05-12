package common;

public class FileInfo {

    private final String name;
    private volatile String editorUsername;

    public FileInfo(String name) {
        this.name = name;
        this.editorUsername = null;
    }

    public String getName() {
        return name;
    }

    public synchronized boolean isFree() {
        return editorUsername == null;
    }

    public synchronized String getEditor() {
        return editorUsername;
    }

    public synchronized boolean tryLock(String username) {
        if (editorUsername != null) return false;
        editorUsername = username;
        return true;
    }

    public synchronized void unlock() {
        editorUsername = null;
    }

    public String getStatus() {
        return isFree() ? Protocol.FREE : Protocol.EDITING;
    }
}