package crypto;

import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
    // fixed extra bytes that both users need to agree upon
    private static final byte[] FIXED_SALT = "TEMPORARY_STRING".getBytes();
    private static final int PBKDF2_ITERATIONS = 65536;

    //methods
    //AES and DES will call with different bits
    private static SecretKeySpec deriveKey(String passphrase, int keyLengthBits, String algorithm) throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), FIXED_SALT, PBKDF2_ITERATIONS, keyLengthBits);
    SecretKey tmp = factory.generateSecret(spec);

    return new SecretKeySpec(tmp.getEncoded(), algorithm);
    }

    public static SecretKeySpec deriveAESKey(String passphrase) throws Exception {
    return deriveKey(passphrase, 128, "AES");
    }

    // java DES requires 8 byte key length where every 8th bit is parity 
    public static SecretKeySpec deriveDESKey(String passphrase) throws Exception {
        return deriveKey(passphrase, 64, "DES");
    }
}
