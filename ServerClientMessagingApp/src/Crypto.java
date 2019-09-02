import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;


public class Crypto {

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey sharedKey;

    //RSA objects generate a key pair for the user and carry out encryption and decryption operations
    public Crypto() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    public void generateSharedKey() throws NoSuchAlgorithmException{
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        sharedKey = kg.generateKey();
    }

    public Message encryptMessageAES(String body, String sender, String recipient) throws Exception{
        SecureRandom srand = new SecureRandom();
        byte[] iv = new byte[16];
        srand.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParam = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE,sharedKey,ivParam);

        SealedObject b = new SealedObject(body,cipher);
        SealedObject s = new SealedObject(sender,cipher);
        SealedObject r = new SealedObject(recipient,cipher);
        byte[] signature = sign(body+sender+recipient);

        return new Message(b,s,r,iv,signature);
    }

    public Command encryptCommandAES(String command, String argument1, String argument2, String sender, String recipient) throws Exception{
        SecureRandom srand = new SecureRandom();
        byte[] iv = new byte[16];
        srand.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParam = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE,sharedKey,ivParam);

        SealedObject c = new SealedObject(command,cipher);
        SealedObject a1 = new SealedObject(argument1,cipher);
        SealedObject a2 = new SealedObject(argument2,cipher);
        SealedObject s = new SealedObject(sender,cipher);
        SealedObject r = new SealedObject(recipient, cipher);
        byte[] signature = sign(command + argument1 + argument2 + sender + recipient);

        return  new Command(c,a1,a2,s,r,signature,iv);

    }

    public String decryptAES(SealedObject cipherText, IvParameterSpec iv) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,sharedKey,iv);
        return (String) cipherText.getObject(cipher);
    }

    public void setSharedKey(SecretKey sharedKey){
        this.sharedKey = sharedKey;
    }

    public SealedObject encryptKey(PublicKey receiverPublicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,receiverPublicKey );

        SealedObject ciphertext = new SealedObject(sharedKey,cipher) ;

        return ciphertext;
    }

    public SecretKey decryptKey(SealedObject cipherText) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE,privateKey);

        return (SecretKey)  cipherText.getObject(cipher);
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

    public PublicKey getPublicKey(){
        return publicKey;
    }

    public byte[] sign(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature s =  Signature.getInstance("SHA256withRSA");
        s.initSign(privateKey);
        s.update(message.getBytes());
        return s.sign();
    }

    public boolean verifyMessageSignature(Message message, PublicKey sendersPublicKey) throws Exception{
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(sendersPublicKey);

        IvParameterSpec iv = message.getIv();
        String body = decryptAES(message.getMessage(),iv);
        String sender = decryptAES(message.getSender(),iv);
        String recipient = decryptAES(message.getRecipient(),iv);
        String combo = body+sender+recipient;

        s.update(combo.getBytes());
        return  s.verify(message.getSignature());
    }

    public boolean verifyCommandSignature(Command com, PublicKey sendersPublicKey) throws Exception{
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(sendersPublicKey);

        IvParameterSpec iv = com.getIvParam();
        String command = decryptAES(com.getCommand(),iv);
        String argument1 = decryptAES(com.getArgument1(),iv) ;
        String argument2 = decryptAES(com.getArgument2(),iv);
        String sender = decryptAES(com.getSender(),iv);
        String recipient = decryptAES(com.getRecipient(),iv);
        String combo = command+argument1+argument2+sender+recipient ;

        s.update(combo.getBytes());
        return  s.verify(com.getSignature());
    }

}
