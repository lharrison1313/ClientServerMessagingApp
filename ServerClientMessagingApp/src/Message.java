import javax.crypto.SealedObject;

public class Message implements java.io.Serializable {

    private SealedObject message, sender, recipient;

    //a message object consists of a message body, a sender and a recipient
    public Message(SealedObject message, SealedObject sender, SealedObject recipient){
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
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

    public void setMessage(SealedObject message) {
        this.message = message;
    }

    public void setRecipient(SealedObject recipient) {
        this.recipient = recipient;
    }

    public void setSender(SealedObject sender) {
        this.sender = sender;
    }
}
