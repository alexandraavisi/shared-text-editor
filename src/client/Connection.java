package client;

import common.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Connection {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private MessageListener listener;
    private Thread listenerThread;

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public interface MessageListener {
        void onMessage(String[] parts, List<String> body);
    }

    public boolean connect(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            startListening();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void send(String... parts) {
        writer.println(String.join(Protocol.SEP, parts));
        writer.flush();
    }


    public void sendWithBody(String header, String content) {
        String[] lines = content.split("\n", -1);
        writer.println(header + Protocol.SEP + lines.length);
        for (String line : lines) {
            writer.println(line);
        }
        writer.flush();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    processIncoming(line);
                }
            } catch (IOException e) {

            } finally {
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void processIncoming(String line) throws IOException {
        String[] parts = line.split("\\" + Protocol.SEP, -1);
        List<String> body = new ArrayList<>();


        String type = parts[0];
        if (type.equals(Protocol.FILE_LIST) ||
                type.equals(Protocol.FILE_CONTENT) ||
                type.equals(Protocol.NOTIFY_SAVE)) {


            int lineCount = 0;
            try {
                lineCount = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}

            if (type.equals(Protocol.FILE_LIST)) {

                for (int i = 0; i < lineCount; i++) {
                    String fileLine = reader.readLine();
                    if (fileLine != null) body.add(fileLine);
                }
            } else {

                for (int i = 0; i < lineCount; i++) {
                    String contentLine = reader.readLine();
                    if (contentLine != null) body.add(contentLine);
                }
            }
        }

        if (listener != null) listener.onMessage(parts, body);
    }
}