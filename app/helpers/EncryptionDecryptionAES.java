package helpers;

import play.Logger;

import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by dimi5963 on 11/28/15.
 */
public class EncryptionDecryptionAES {
    static Cipher cipher;
    static SecretKey secretKey;

    static {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            secretKey = keyGenerator.generateKey();
            cipher = Cipher.getInstance("AES");
        }catch(NoSuchAlgorithmException | NoSuchPaddingException ex){
            ex.printStackTrace();
            Logger.error("We got an error: " + ex.getLocalizedMessage());
        }
    }

    public static String encrypt(String plainText)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Logger.info("encrypt: " + plainText);
        byte[] plainTextByte = plainText.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    public static String decrypt(String encryptedText)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Logger.info("decrypt: " + encryptedText);
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }
}
