import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;

/*
    Purpose: The purpose of this class is store the data needed for a command in order to more easily transport it
    in an object output stream. It stores the following: the command that needs to be carried out, the sender of the command,
    the recipient of the command, the commands argument, and the commands signature.
 */

public class Command implements java.io.Serializable {

    private SealedObject command, sender, recipient, argument1, argument2;
    private byte[] signature, iv;


    public Command(SealedObject command, SealedObject argument1, SealedObject argument2, SealedObject sender, SealedObject recipient, byte[] signature, byte[] iv){
        this.command = command;
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.sender = sender;
        this.recipient = recipient;
        this.signature = signature;
        this.iv = iv;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public SealedObject getArgument2() {
        return argument2;
    }

    public void setArgument2(SealedObject argument2) {
        this.argument2 = argument2;
    }

    public SealedObject getArgument1() {
        return argument1;
    }

    public void setArgument1(SealedObject argument) {
        this.argument1 = argument;
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


    public IvParameterSpec getIvParam(){
        return new IvParameterSpec(iv);
    }

    public byte[] getSignature(){
        return signature;
    }
}
