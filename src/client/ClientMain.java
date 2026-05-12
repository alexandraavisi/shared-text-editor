package client;

import javax.swing.*;
import java.awt.*;

public class ClientMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientMain::showLoginDialog);
    }

    private static void showLoginDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Shared Text Editor — Conectare");
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Server host:"), gbc);
        gbc.gridx = 1;
        JTextField hostField = new JTextField("localhost", 15);
        dialog.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        JTextField portField = new JTextField("5000", 15);
        dialog.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField userField = new JTextField(15);
        dialog.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton connectBtn = new JButton("Conectare");
        dialog.add(connectBtn, gbc);

        connectBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            String username = userField.getText().trim();
            int port;

            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Port invalid!");
                return;
            }

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Introdu un username!");
                return;
            }

            Connection connection = new Connection();
            boolean ok = connection.connect(host, port);

            if (!ok) {
                JOptionPane.showMessageDialog(dialog,
                        "Nu ma pot conecta la " + host + ":" + port);
                return;
            }

            dialog.dispose();
            new EditorWindow(connection, username).show();
        });

        dialog.setVisible(true);
    }
}