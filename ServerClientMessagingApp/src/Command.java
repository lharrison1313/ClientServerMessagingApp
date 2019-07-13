import javax.crypto.SealedObject;

public class Command implements java.io.Serializable {

    private SealedObject command, sender, recipient,option, signature;

    //command object consist of a command, an option for that command, a sender and a recipient
    public Command(SealedObject command,SealedObject signature, SealedObject option, SealedObject sender, SealedObject recipient) {
        this.command = command;
        this.signature = signature;
        this.sender = sender;
        this.recipient = recipient;
        this.option = option;
    }

    public SealedObject getOption() {
        return option;
    }

    public void setOption(SealedObject option) {
        this.option = option;
    }

    public SealedObject getCommand() {
        return command;
    }


    public void setCommand(SealedObject command) {
        this.command = command;
    }


    public SealedObject getSender() {
        return sender;
    }

    public void setSender(SealedObject sender) {
        this.sender = sender;
    }

    public SealedObject getRecipient() {
        return recipient;
    }

    public void setRecipient(SealedObject recipient) {
        this.recipient = recipient;
    }

    public SealedObject getSignature() {
        return signature;
    }

    public void setSignature(SealedObject signature) {
        this.signature = signature;
    }
}
