import GUI.ChatGUI;
import crypto.CryptoEngine;
import crypto.KeyManager;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.SwingUtilities;
import network.PeerConnection;

public class Main {
    public static void main(String[] args) throws Exception {
        PeerConnection pc = new PeerConnection();

        String passphrase = "sharedpassword"; // TODO: prompt for this instead of hardcoding
        boolean use128bit = true; // TODO: let user pick this instead of hardcoding

        SecretKeySpec key = use128bit ? KeyManager.deriveAESKey(passphrase) : KeyManager.deriveDESKey(passphrase);
        String transformation = use128bit ? CryptoEngine.AES_TRANSFORMATION : CryptoEngine.DES_TRANSFORMATION;

        if (args.length > 0 && args[0].equals("listen")) {
            pc.listen(5050);
        } else {
            String host = args.length > 0 ? args[0] : "localhost";
            pc.connect(host, 5050);
        }

        SwingUtilities.invokeLater(() -> new ChatGUI(pc, key, transformation));
    }
}