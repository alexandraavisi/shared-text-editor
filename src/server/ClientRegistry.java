package server;

import common.Protocol;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {

    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public boolean register(String username, ClientHandler handler) {
        if (clients.containsKey(username)) return false;
        clients.put(username, handler);
        return true;
    }

    public void unregister(String username) {
        clients.remove(username);
    }

    public Collection<ClientHandler> getAll() {
        return Collections.unmodifiableCollection(clients.values());
    }

    public int count() {
        return clients.size();
    }

    public void broadcast(String message, String excludeUser) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (excludeUser != null && excludeUser.equals(entry.getKey())) continue;
            entry.getValue().sendNotification(message);
        }
    }

    public void broadcastAll(String message) {
        broadcast(message, null);
    }

    public void broadcastToViewers(String filename, String excludeUser, List<String> lines) {
        String header = Protocol.NOTIFY_SAVE + Protocol.SEP + filename + Protocol.SEP + lines.size();
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (excludeUser != null && excludeUser.equals(entry.getKey())) continue;
            ClientHandler h = entry.getValue();
            if (h.isViewing(filename)) {
                h.sendNotificationWithBody(header, lines);
            }
        }
    }
}
