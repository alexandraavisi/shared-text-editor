package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class ServerMain {

    private static final Logger log = Logger.getLogger(ServerMain.class.getName());
    private static final int PORT = 5000;
    private static final String FILES_DIR = "server-files";

    public static void main(String[] args) {
        FileManager fileManager;
        try {
            fileManager = new FileManager(FILES_DIR);
        } catch (IOException e) {
            System.err.println("Nu pot initializa directorul: " + e.getMessage());
            return;
        }

        ClientRegistry registry = new ClientRegistry();

        startFileWatcher(fileManager, registry);

        System.out.println("Server pornit pe portul " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client nou conectat: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, fileManager, registry);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Eroare server: " + e.getMessage());
        }
    }

    private static void startFileWatcher(FileManager fileManager, ClientRegistry registry) {
        Thread watcher = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);

                    List<String> added = fileManager.detectAdded();
                    for (String name : added) {
                        System.out.println("Fisier adaugat: " + name);
                        registry.broadcastAll(
                                common.Protocol.NOTIFY_ADD + common.Protocol.SEP + name
                        );
                    }

                    List<String> deleted = fileManager.detectDeleted();
                    for (String name : deleted) {
                        System.out.println("Fisier sters: " + name);
                        registry.broadcastAll(
                                common.Protocol.NOTIFY_DELETE + common.Protocol.SEP + name
                        );
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Eroare file watcher: " + e.getMessage());
                }
            }
        });
        watcher.setDaemon(true);
        watcher.start();
    }
}