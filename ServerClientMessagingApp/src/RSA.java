import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.util.Arrays;

public class RSA {

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyPairGenerator kpg;

    //RSA objects generate a key pair for the user and carry out encryption and decryption operations
    //for security reasons the users private key is not made available
    public RSA() throws NoSuchAlgorithmException, NoSuchPaddingException {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    //encrypts a string plaintext with a users public key
    public SealedObject encrypt(String message, PublicKey receiverPublicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,receiverPublicKey );

        SealedObject ciphertext = new SealedObject(message,cipher);

        return ciphertext;
    }

    //decrypts a sealed object ciphertext using the users private key
    public  String decrypt(SealedObject cipherText) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE,privateKey);

        return (String)  cipherText.getObject(cipher);
    }

    //hashes a string
    public byte[] hashString(String message) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(message.getBytes());
    }

    public static byte[] generateSalt(int size){
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[size];
        sr.nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt, int iterationCount) throws Exception{
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(),salt,iterationCount,512);
        SecretKey skey = skf.generateSecret(keySpec);
        return skey.getEncoded();

    }

    //hashes a string and encrypts the byte array object with senders private key
    public SealedObject sign(String message) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,privateKey);
        byte[] messageHash = hashString(message);
        return new SealedObject(messageHash,cipher);

    }

    //decrypts a Sealed byte array with senders public key
    private byte[] hashDecrypt(SealedObject hash, PublicKey sendersPublicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE,sendersPublicKey);
        return (byte[]) hash.getObject(cipher);
    }

    //compares the hash received with a hash of the received message message
    public boolean verifySignature(SealedObject message, SealedObject hash, PublicKey sendersPublicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException{
        String messageDec = decrypt(message);
        byte[] hashDec =  hashDecrypt(hash,sendersPublicKey);
        return Arrays.equals(hashDec,hashString(messageDec));
    }

    public PublicKey getPublicKey(){
        return publicKey;
    }

}
