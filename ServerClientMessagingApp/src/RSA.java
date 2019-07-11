
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.*;
import javax.crypto.*;

public class RSA {

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyPairGenerator kpg;


    public RSA() throws NoSuchAlgorithmException, NoSuchPaddingException {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    public SealedObject encrypt(String message, PublicKey receiverPublicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher1.init(Cipher.ENCRYPT_MODE,receiverPublicKey );

        SealedObject ciphertext = new SealedObject(message,cipher1);

        return ciphertext;
    }

    public  String decrypt(SealedObject cipherText) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, BadPaddingException{
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher1.init(Cipher.DECRYPT_MODE,privateKey);

        return (String)  cipherText.getObject(cipher1);
    }


    public PublicKey getPublicKey(){
        return publicKey;
    }


}
