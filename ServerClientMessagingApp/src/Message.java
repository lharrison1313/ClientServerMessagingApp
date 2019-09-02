import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;

public class Message implements java.io.Serializable {

    private SealedObject message, sender, recipient;
    private byte[] signature,iv;

    public Message(SealedObject message, SealedObject sender, SealedObject recipient, byte[] iv, byte[] signature){
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
        this.iv = iv;
        this.signature = signature;
    }

    public IvParameterSpec getIv(){
        return new IvParameterSpec(iv);
    }

    public SealedObject getMessage() {
        return message;
    }

    public SealedObject getRecipient() {
        return recipient;
    }

    public SealedObject getSender() {
        return sender;
    }

    public byte[] getSignature(){
        return signature;
    }

    public void setIv(byte[] iv){
        this.iv = iv;
    }

    public void setMessage(SealedObject message) {
        this.message = message;
    }

    public void setSignature(byte[] signature){ this.signature = signature;}

    public void setRecipient(SealedObject recipient) {
        this.recipient = recipient;
    }

    public void setSender(SealedObject sender) {
        this.sender = sender;
    }
}
