package network;

import crypto.CryptoEngine;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.spec.SecretKeySpec;
import protocol.MessageProtocol;
import protocol.MessageProtocol.MessageType;

public class PeerConnection {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public void listen(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for connection on port " + port + "...");
            socket = serverSocket.accept();
            System.out.println("Connected!");
        }
        setupStreams();
    }

    public void connect(String host, int port) throws IOException {
        System.out.println("Connecting to " + host + ":" + port + "...");
        socket = new Socket(host, port);
        System.out.println("Connected!");
        setupStreams();
    }

    private void setupStreams() throws IOException {
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void sendEncrypted(MessageType type, byte[] plaintext, SecretKeySpec key, String transformation) throws Exception {
        byte[] ivAndCiphertext = CryptoEngine.encrypt(plaintext, key, transformation);
        MessageProtocol.writeFrame(out, type, ivAndCiphertext);
    }

    public static class ReceivedMessage {
        public final MessageType type;
        public final byte[] plaintext;
        public ReceivedMessage(MessageType type, byte[] plaintext) {
            this.type = type;
            this.plaintext = plaintext;
        }
    }

    public ReceivedMessage receiveEncrypted(SecretKeySpec key, String transformation) throws Exception {
        MessageProtocol.Frame frame = MessageProtocol.readFrame(in);
        byte[] plaintext = CryptoEngine.decrypt(frame.payload, key, transformation);
        return new ReceivedMessage(frame.type, plaintext);
    }
}