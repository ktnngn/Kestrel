package crypto;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoEngine {

    public static final String DES_TRANSFORMATION = "DES/CBC/PKCS5Padding";
    public static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static byte[] encrypt(byte[] plaintext, SecretKeySpec key, String transformation) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        int blockSize = cipher.getBlockSize();
        byte[] iv = new byte[blockSize];
        new SecureRandom().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        // prepend the IV to the ciphertext so the receiver can pull it back out
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    public static byte[] decrypt(byte[] ivAndCiphertext, SecretKeySpec key, String transformation) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        int blockSize = cipher.getBlockSize();

        byte[] iv = new byte[blockSize];
        byte[] ciphertext = new byte[ivAndCiphertext.length - blockSize];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, blockSize);
        System.arraycopy(ivAndCiphertext, blockSize, ciphertext, 0, ciphertext.length);

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }
}
