package GUI;

import crypto.CryptoEngine;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import network.PeerConnection;
import protocol.MessageProtocol.MessageType;

public class ChatGUI {
    private final PeerConnection pc;
    private final SecretKeySpec key;
    private final String transformation;

    private JPanel chatPanel;
    private JScrollPane chatScroll;
    private JTextArea ciphertextView;
    private JTextField inputField;
    private final File outDir;

    public ChatGUI(PeerConnection pc, SecretKeySpec key, String transformation) {
        this.pc = pc;
        this.key = key;
        this.transformation = transformation;
        this.outDir = new File(System.getProperty("java.io.tmpdir"), "kestrel");
        outDir.mkdirs();
        buildUI();
        startReceiveLoop();
    }

    private void buildUI() {
        JFrame frame = new JFrame("Kestrel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 600);
        frame.setLayout(new BorderLayout());

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatScroll = new JScrollPane(chatPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        ciphertextView = new JTextArea();
        ciphertextView.setEditable(false);
        JScrollPane cipherScroll = new JScrollPane(ciphertextView);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chatScroll, cipherScroll);
        splitPane.setResizeWeight(0.7);
        frame.add(splitPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendText());

        JButton attachBtn = new JButton("+");
        attachBtn.addActionListener(e -> showAttachMenu(attachBtn));

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendText());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(attachBtn, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void showAttachMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem photoItem = new JMenuItem("Photo");
        photoItem.addActionListener(e -> sendFileAs(MessageType.PIC));
        JMenuItem voiceItem = new JMenuItem("Voice Memo");
        voiceItem.addActionListener(e -> sendFileAs(MessageType.VOICE));
        JMenuItem fileItem = new JMenuItem("File");
        fileItem.addActionListener(e -> sendFileAs(MessageType.FILE));
        menu.add(photoItem);
        menu.add(voiceItem);
        menu.add(fileItem);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void sendText() {
    String text = inputField.getText();
    if (text.isEmpty()) return;
    inputField.setText("");
    new Thread(() -> {
        try {
            SwingUtilities.invokeAndWait(() -> addTextBubble("You", text));
            byte[] plaintext = text.getBytes();
            pc.sendEncrypted(MessageType.TEXT, plaintext, key, transformation);
            SwingUtilities.invokeLater(() -> logCiphertext("[TEXT] " + text, plaintext));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> showError(ex));
        }
    }).start();
}

  private void sendFileAs(MessageType type) {
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(null);
    if (result != JFileChooser.APPROVE_OPTION) return;

    File file = chooser.getSelectedFile();
    new Thread(() -> {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] plaintext = packWithFilename(file.getName(), fileBytes);

            SwingUtilities.invokeAndWait(() -> addAttachmentBubble("You", type, file.getName(), fileBytes));
            pc.sendEncrypted(type, plaintext, key, transformation);
            SwingUtilities.invokeLater(() -> logCiphertext("[" + type + "] " + file.getName() + " (" + fileBytes.length + " bytes)", plaintext));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> showError(ex));
        }
    }).start();
}

private byte[] packWithFilename(String filename, byte[] data) throws IOException {
    byte[] nameBytes = filename.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(nameBytes.length);
    dos.write(nameBytes);
    dos.write(data);
    return baos.toByteArray();
}

    private void logCiphertext(String label, byte[] plaintext) {
        try {
            byte[] ciphertext = CryptoEngine.encrypt(plaintext, key, transformation);
            ciphertextView.append(label + " -> " + bytesToHex(ciphertext) + "\n");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void startReceiveLoop() {
        Thread receiveThread = new Thread(() -> {
            while (true) {
                try {
                    PeerConnection.ReceivedMessage msg = pc.receiveEncrypted(key, transformation);
                    handleReceived(msg);
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex));
                    break;
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void handleReceived(PeerConnection.ReceivedMessage msg) {
    SwingUtilities.invokeLater(() -> {
        if (msg.type == MessageType.TEXT) {
            addTextBubble("Them", new String(msg.plaintext));
        } else {
            try {
                Object[] unpacked = unpackFilename(msg.plaintext);
                String filename = (String) unpacked[0];
                byte[] fileData = (byte[]) unpacked[1];
                addAttachmentBubble("Them", msg.type, filename, fileData);
            } catch (Exception ex) {
                showError(ex);
            }
        }
    });
}

private Object[] unpackFilename(byte[] packed) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(packed);
    DataInputStream dis = new DataInputStream(bais);
    int nameLength = dis.readInt();
    byte[] nameBytes = new byte[nameLength];
    dis.readFully(nameBytes);
    String filename = new String(nameBytes, StandardCharsets.UTF_8);
    byte[] fileData = dis.readAllBytes();
    return new Object[]{filename, fileData};
}

    private void addTextBubble(String who, String text) {
        JLabel label = new JLabel("<html><b>" + who + ":</b> " + escapeHtml(text) + "</html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatPanel.add(label);
        chatPanel.add(Box.createVerticalStrut(6));
        refreshChat();
    }

    private void addAttachmentBubble(String who, MessageType type, String filename, byte[] data) {
    try {
        File outFile = new File(outDir, System.currentTimeMillis() + "_" + safeFileName(filename));
        Files.write(outFile.toPath(), data);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel(who + " sent a " + type.toString().toLowerCase() + ":");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(header);

        switch (type) {
            case PIC -> {
                ImageIcon icon = new ImageIcon(data);
                Image scaled = icon.getImage().getScaledInstance(220, -1, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                bubble.add(imageLabel);
            }
            case VOICE -> {
                JButton playBtn = new JButton("Play voice memo");
                playBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                playBtn.addActionListener(e -> playAudio(outFile));
                bubble.add(playBtn);
            }
            default -> {
                JButton openBtn = new JButton("Open " + outFile.getName());
                openBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                openBtn.addActionListener(e -> openFile(outFile));
                bubble.add(openBtn);
            }
        }

        JButton saveAsBtn = new JButton("Save As...");
        saveAsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveAsBtn.addActionListener(e -> saveAs(filename, data));
        bubble.add(saveAsBtn);

        chatPanel.add(bubble);
        chatPanel.add(Box.createVerticalStrut(6));
        refreshChat();
    } catch (Exception ex) {
        showError(ex);
    }
}

    private void playAudio(File file) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                "Couldn't auto-play this audio format. Saved at:\n" + file.getAbsolutePath());
        }
    }

    private void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void refreshChat() {
        chatPanel.revalidate();
        chatPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private String safeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
    }

    private void saveAs(String suggestedName, byte[] data) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(suggestedName));
        int result = chooser.showSaveDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        try {
            File target = chooser.getSelectedFile();
            Files.write(target.toPath(), data);
            JOptionPane.showMessageDialog(null, "Saved to: " + target.getAbsolutePath());
        } catch (IOException ex) {
            showError(ex);
        }
}
}