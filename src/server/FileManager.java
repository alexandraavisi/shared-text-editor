package server;

import common.FileInfo;
import common.Protocol;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {

    private final Path directory;
    private final ConcurrentHashMap<String, FileInfo> files = new ConcurrentHashMap<>();

    public FileManager(String dirPath) throws IOException {
        this.directory = Paths.get(dirPath);
        if (!Files.exists(this.directory)) {
            Files.createDirectories(this.directory);
        }
        loadExistingFiles();
    }

    private void loadExistingFiles() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                files.put(name, new FileInfo(name));
            }
        }
    }

    public Collection<FileInfo> getAllFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    public FileInfo getFile(String name) {
        return files.get(name);
    }

    public boolean exists(String name) {
        return files.containsKey(name);
    }

    public List<String> readFile(String name) throws IOException {
        Path path = directory.resolve(name);
        return Files.readAllLines(path);
    }

    public void writeFile(String name, String content) throws IOException {
        Path path = directory.resolve(name);
        Files.writeString(path, content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<String> detectAdded() throws IOException {
        List<String> added = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!files.containsKey(name)) {
                    files.put(name, new FileInfo(name));
                    added.add(name);
                }
            }
        }
        return added;
    }

    public List<String> detectDeleted() {
        List<String> deleted = new ArrayList<>();
        for (String name : files.keySet()) {
            if (!Files.exists(directory.resolve(name))) {
                files.remove(name);
                deleted.add(name);
            }
        }
        return deleted;
    }
}