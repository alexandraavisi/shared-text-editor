package client;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class EditorWindow {

    private final Connection connection;
    private final String username;

    private JFrame frame;
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JTextArea editorArea;
    private JLabel statusLabel;
    private JLabel fileStatusLabel;
    private JButton viewBtn, editBtn, saveBtn, releaseBtn;

    private String selectedFile;
    private String editingFile;
    private boolean isEditing = false;

    public EditorWindow(Connection connection, String username) {
        this.connection = connection;
        this.username = username;
    }

    public void show() {
        SwingUtilities.invokeLater(this::buildUI);
    }


    private void buildUI() {
        connection.setListener(this::handleMessage);

        frame = new JFrame("Shared Text Editor — " + username);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (editingFile != null) {
                    connection.send(Protocol.RELEASE, editingFile);
                }
                connection.send(Protocol.DISCONNECT);
                connection.disconnect();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setLayout(new BorderLayout());


        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setBorder(new EmptyBorder(8, 8, 8, 4));

        JLabel filesLabel = new JLabel("Fișiere disponibile");
        filesLabel.setFont(filesLabel.getFont().deriveFont(Font.BOLD));
        leftPanel.add(filesLabel, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedFile = extractFilename(fileList.getSelectedValue());
                updateButtons();
            }
        });

        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        frame.add(leftPanel, BorderLayout.WEST);


        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(8, 4, 8, 8));


        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fileStatusLabel = new JLabel("Selectează un fișier");
        fileStatusLabel.setFont(fileStatusLabel.getFont().deriveFont(Font.ITALIC));
        toolbar.add(fileStatusLabel);
        toolbar.add(Box.createHorizontalStrut(20));

        viewBtn = new JButton("Vizualizare");
        editBtn = new JButton("Editare");
        saveBtn = new JButton("Salvare");
        releaseBtn = new JButton("Renunță");

        viewBtn.setEnabled(false);
        editBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        releaseBtn.setEnabled(false);

        toolbar.add(viewBtn);
        toolbar.add(editBtn);
        toolbar.add(saveBtn);
        toolbar.add(releaseBtn);

        rightPanel.add(toolbar, BorderLayout.NORTH);


        editorArea = new JTextArea();
        editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        editorArea.setEditable(false);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        rightPanel.add(new JScrollPane(editorArea), BorderLayout.CENTER);

        frame.add(rightPanel, BorderLayout.CENTER);


        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statusLabel = new JLabel("Conectat ca: " + username);
        statusBar.add(statusLabel);
        frame.add(statusBar, BorderLayout.SOUTH);


        viewBtn.addActionListener(e -> doView());
        editBtn.addActionListener(e -> doEdit());
        saveBtn.addActionListener(e -> doSave());
        releaseBtn.addActionListener(e -> doRelease());


        connection.startListening();
        connection.send(Protocol.CONNECT, username);

        frame.setVisible(true);
    }

    private void doView() {
        if (selectedFile == null) return;
        isEditing = false;
        connection.send(Protocol.VIEW, selectedFile);
    }

    private void doEdit() {
        if (selectedFile == null) return;
        if (editingFile != null) return;
        isEditing = true;
        editingFile = selectedFile;
        editBtn.setEnabled(false);
        viewBtn.setEnabled(false);
        connection.send(Protocol.EDIT, selectedFile);
        updateButtons();
    }

    private void doSave() {
        if (editingFile == null) return;
        String content = editorArea.getText();
        connection.sendWithBody(
                Protocol.SAVE + Protocol.SEP + editingFile,
                content
        );
    }

    private void doRelease() {
        if (editingFile == null) return;
        connection.send(Protocol.RELEASE, editingFile);
        editingFile = null;
        isEditing = false;
        editorArea.setEditable(false);
        editorArea.setText("");
        fileStatusLabel.setText("Selectează un fișier");
        updateButtons();
    }


    private void handleMessage(String[] parts, List<String> body) {
        SwingUtilities.invokeLater(() -> {
            switch (parts[0]) {
                case Protocol.FILE_LIST -> updateFileList(body);
                case Protocol.FILE_CONTENT -> showFileContent(parts, body);
                case Protocol.OK -> handleOk();
                case Protocol.ERROR -> showError(parts);
                case Protocol.NOTIFY_EDIT -> handleNotifyEdit(parts);
                case Protocol.NOTIFY_RELEASE -> handleNotifyRelease(parts);
                case Protocol.NOTIFY_SAVE -> handleNotifySave(parts, body);
                case Protocol.NOTIFY_ADD -> handleNotifyAdd(parts);
                case Protocol.NOTIFY_DELETE -> handleNotifyDelete(parts);
            }
        });
    }

    private void updateFileList(List<String> body) {
        fileListModel.clear();
        for (String line : body) {
            String[] p = line.split("\\" + Protocol.SEP, 3);
            if (p.length >= 2) {
                String display;
                if (Protocol.FREE.equals(p[1])) {
                    display = p[0] + "  [liber]";
                } else {
                    String editor = p.length >= 3 ? p[2] : "?";
                    display = p[0] + "  [editat de " + editor + "]";
                }
                fileListModel.addElement(display);
            }
        }
    }

    private void showFileContent(String[] parts, List<String> body) {

        if (parts.length >= 2) {
            String filename = parts[1];
            editorArea.setText(String.join("\n", body));
            editorArea.setCaretPosition(0);

            if (isEditing && filename.equals(editingFile)) {
                editorArea.setEditable(true);
                fileStatusLabel.setText(filename + " — în editare");
            } else {
                editorArea.setEditable(false);
                editingFile = null;
                isEditing = false;
                fileStatusLabel.setText(filename + " — vizualizare");
            }
            updateButtons();
        }
    }

    private void handleOk() {
        if (editingFile != null && !isEditing) {

            editingFile = null;
            isEditing = false;
            editorArea.setEditable(false);
            editorArea.setText("");
            fileStatusLabel.setText("Fișier eliberat");
            updateButtons();
        }
    }

    private void showError(String[] parts) {
        String msg = parts.length >= 2 ? parts[1] : "Eroare necunoscuta";
        JOptionPane.showMessageDialog(frame, msg, "Eroare", JOptionPane.ERROR_MESSAGE);
    }

    private void handleNotifyEdit(String[] parts) {
        if (parts.length >= 3) {
            String filename = parts[1];
            String editor = parts[2];
            updateFileStatusInList(filename, "editat de " + editor);
            statusLabel.setText("'" + filename + "' preluat în editare de " + editor);
        }
    }

    private void handleNotifyRelease(String[] parts) {
        if (parts.length >= 2) {
            String filename = parts[1];
            updateFileStatusInList(filename, "liber");
            statusLabel.setText("'" + filename + "' este din nou liber");

            if (filename.equals(editingFile)) {
                editingFile = null;
                isEditing = false;
                editorArea.setEditable(false);
                updateButtons();
            }
        }
    }

    private void handleNotifySave(String[] parts, List<String> body) {
        if (parts.length >= 2) {
            String filename = parts[1];
            if (filename.equals(selectedFile)) {
                editorArea.setText(String.join("\n", body));
                editorArea.setCaretPosition(0);
                statusLabel.setText("'" + filename + "' actualizat de editor");
            }
        }
    }

    private void handleNotifyAdd(String[] parts) {
        if (parts.length >= 2) {
            String filename = parts[1];
            fileListModel.addElement(filename + "  [liber]");
            statusLabel.setText("Fișier nou adăugat: " + filename);
        }
    }

    private void handleNotifyDelete(String[] parts) {
        if (parts.length >= 2) {
            String filename = parts[1];
            for (int i = 0; i < fileListModel.size(); i++) {
                if (fileListModel.get(i).startsWith(filename)) {
                    fileListModel.remove(i);
                    break;
                }
            }
            statusLabel.setText("Fișier șters: " + filename);
        }
    }

    private void updateButtons() {
        boolean fileSelected = selectedFile != null;
        boolean iAmEditing = editingFile != null;

        viewBtn.setEnabled(fileSelected && !iAmEditing);
        editBtn.setEnabled(fileSelected && !iAmEditing);
        saveBtn.setEnabled(iAmEditing);
        releaseBtn.setEnabled(iAmEditing);
    }

    private void updateFileStatusInList(String filename, String status) {
        for (int i = 0; i < fileListModel.size(); i++) {
            if (fileListModel.get(i).startsWith(filename)) {
                fileListModel.set(i, filename + "  [" + status + "]");
                break;
            }
        }
    }

    private String extractFilename(String displayValue) {
        if (displayValue == null) return null;
        int idx = displayValue.indexOf("  [");
        return idx > 0 ? displayValue.substring(0, idx) : displayValue;
    }
}