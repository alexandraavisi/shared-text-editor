package server;

import common.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private static final Logger log = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final FileManager fileManager;
    private final ClientRegistry registry;

    private BufferedReader reader;
    private PrintWriter writer;

    private String username;
    private String currentlyViewing;  // fisierul vizualizat curent
    private String currentlyEditing;  // fisierul in editare curent

    public ClientHandler(Socket socket, FileManager fileManager, ClientRegistry registry) {
        this.socket = socket;
        this.fileManager = fileManager;
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            handleConnect();
            if (username == null) return;

            String line;
            while ((line = reader.readLine()) != null) {
                if (!processCommand(line)) break;
            }

        } catch (IOException e) {
            log.info("Client deconectat: " + (username != null ? username : "neautentificat"));
        } finally {
            cleanup();
        }
    }

    // -------------------------------------------------------
    // Autentificare
    // -------------------------------------------------------

    private void handleConnect() throws IOException {
        String line = reader.readLine();
        if (line == null) return;

        String[] parts = line.split("\\" + Protocol.SEP, 2);
        if (!Protocol.CONNECT.equals(parts[0]) || parts.length < 2 || parts[1].isBlank()) {
            sendMessage(Protocol.ERROR, "Primul mesaj trebuie sa fie CONNECT|<username>");
            return;
        }

        String name = parts[1].trim();
        if (!registry.register(name, this)) {
            sendMessage(Protocol.ERROR, "Username deja folosit.");
            return;
        }

        username = name;
        log.info("Client autentificat: " + username);

        // Trimite lista fisierelor
        sendFileList();
    }

    // -------------------------------------------------------
    // Procesare comenzi
    // -------------------------------------------------------

    private boolean processCommand(String line) throws IOException {
        String[] parts = line.split("\\" + Protocol.SEP, 2);
        String cmd = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case Protocol.LIST:
                sendFileList();
                break;
            case Protocol.VIEW:
                handleView(arg);
                break;
            case Protocol.EDIT:
                handleEdit(arg);
                break;
            case Protocol.SAVE:
                handleSave(arg);
                break;
            case Protocol.RELEASE:
                handleRelease(arg);
                break;
            case Protocol.DISCONNECT:
                return false;
            default:
                sendMessage(Protocol.ERROR, "Comanda necunoscuta: " + cmd);
        }
        return true;
    }

    // -------------------------------------------------------
    // Handlers comenzi
    // -------------------------------------------------------

    private void handleView(String filename) throws IOException {
        if (!fileManager.exists(filename)) {
            sendMessage(Protocol.ERROR, "Fisierul nu exista: " + filename);
            return;
        }
        currentlyViewing = filename;
        List<String> lines = fileManager.readFile(filename);
        sendWithBody(Protocol.FILE_CONTENT + Protocol.SEP + filename, lines);
    }

    private void handleEdit(String filename) {
        if (!fileManager.exists(filename)) {
            sendMessage(Protocol.ERROR, "Fisierul nu exista: " + filename);
            return;
        }

        boolean locked = fileManager.getFile(filename).tryLock(username);
        if (!locked) {
            String editor = fileManager.getFile(filename).getEditor();
            sendMessage(Protocol.ERROR, "Fisierul este deja in editare de: " + editor);
            return;
        }

        currentlyEditing = filename;

        try {
            List<String> lines = fileManager.readFile(filename);
            sendWithBody(Protocol.FILE_CONTENT + Protocol.SEP + filename, lines);
        } catch (IOException e) {
            sendMessage(Protocol.ERROR, "Nu pot citi fisierul.");
            return;
        }

        // Notifica ceilalti clienti
        registry.broadcast(
                Protocol.NOTIFY_EDIT + Protocol.SEP + filename + Protocol.SEP + username,
                username
        );
    }

    private void handleSave(String arg) throws IOException {
        // Format: <filename>|<linecount>
        String[] parts = arg.split("\\" + Protocol.SEP, 2);
        if (parts.length < 2) {
            sendMessage(Protocol.ERROR, "Format SAVE invalid.");
            return;
        }

        String filename = parts[0];
        int lineCount;
        try {
            lineCount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            sendMessage(Protocol.ERROR, "Numar de linii invalid.");
            return;
        }

        if (!filename.equals(currentlyEditing)) {
            sendMessage(Protocol.ERROR, "Nu esti in editarea acestui fisier.");
            return;
        }

        // Citeste continutul
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            String line = reader.readLine();
            if (line == null) throw new IOException("Body incomplet.");
            if (i > 0) sb.append('\n');
            sb.append(line);
        }

        String content = sb.toString();
        fileManager.writeFile(filename, content);
        sendMessage(Protocol.OK);

        // Notifica vizualizatorii cu noul continut
        List<String> lines = Arrays.asList(content.split("\n", -1));
        registry.broadcastToViewers(filename, username, lines);
    }

    private void handleRelease(String filename) {
        if (!filename.equals(currentlyEditing)) {
            sendMessage(Protocol.ERROR, "Nu esti in editarea acestui fisier.");
            return;
        }

        fileManager.getFile(filename).unlock();
        currentlyEditing = null;
        sendMessage(Protocol.OK);

        // Notifica toti clientii ca fisierul e liber
        registry.broadcastAll(Protocol.NOTIFY_RELEASE + Protocol.SEP + filename);
    }

    // -------------------------------------------------------
    // Curatare la deconectare
    // -------------------------------------------------------

    private void cleanup() {
        // Elibereaza fisierul daca era in editare
        if (currentlyEditing != null) {
            fileManager.getFile(currentlyEditing).unlock();
            registry.broadcastAll(
                    Protocol.NOTIFY_RELEASE + Protocol.SEP + currentlyEditing
            );
            log.info("Fisier eliberat automat: " + currentlyEditing);
        }

        if (username != null) {
            registry.unregister(username);
            log.info("Client deconectat: " + username);
        }

        try { socket.close(); } catch (IOException ignored) {}
    }

    // -------------------------------------------------------
    // Utilitare trimitere mesaje
    // -------------------------------------------------------

    private void sendMessage(String... parts) {
        writer.println(String.join(Protocol.SEP, parts));
        writer.flush();
    }

    private void sendFileList() {
        var files = fileManager.getAllFiles();
        writer.println(Protocol.FILE_LIST + Protocol.SEP + files.size());
        for (var f : files) {
            String editor = f.isFree() ? Protocol.NO_EDITOR : f.getEditor();
            writer.println(f.getName() + Protocol.SEP + f.getStatus() + Protocol.SEP + editor);
        }
        writer.flush();
    }

    private void sendWithBody(String header, List<String> lines) {
        writer.println(header + Protocol.SEP + lines.size());
        for (String line : lines) {
            writer.println(line);
        }
        writer.flush();
    }

    // Apelat din ClientRegistry pentru notificari push
    public void sendNotification(String message) {
        writer.println(message);
        writer.flush();
    }

    public void sendNotificationWithBody(String header, List<String> lines) {
        writer.println(header);
        for (String line : lines) {
            writer.println(line);
        }
        writer.flush();
    }

    // Verificare daca clientul vizualizeaza un anumit fisier
    public boolean isViewing(String filename) {
        return filename.equals(currentlyViewing);
    }
}