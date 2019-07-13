import javax.crypto.SealedObject;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    //declaring socket, stream variables, and client info
    private Socket s;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String userName;
    private boolean online;
    private Thread cms;
    private Thread cmr;
    private Map<String, User> userMap;
    private ArrayList<String> userNameList;
    private RSA rsaUtil;

    public Client(String host, int port) throws Exception {

        online = true;

        //connects client to server and initializes data streams
        s = new Socket(host,port);

        input = new ObjectInputStream(s.getInputStream());
        output = new ObjectOutputStream(s.getOutputStream());

        //initializing public key hash map and rsa utility
        userMap = new HashMap<>();
        userNameList = new ArrayList<String>();

        try{
            rsaUtil = new RSA();
        }
        catch(Exception e){
            System.out.println(e);
        }

        //sends server username information and checks to see if username is taken
        String usernameSuccess;
        Scanner scan = new Scanner(System.in);

        do{
            System.out.println("please enter your username");
            this.userName = scan.nextLine();
            output.writeObject(this.userName);
            usernameSuccess = (String) input.readObject();
            if(!usernameSuccess.equals("true")){
                System.out.println("username is taken or invalid please choose another");
            }
        }while(!usernameSuccess.equals("true"));

        //setting public key for self and sending public key to server
        User u = new User(userName,rsaUtil.getPublicKey());
        userNameList.add(userName);
        userMap.put(userName,u);
        output.writeObject(u);



        //displays startup info
        System.out.println("type $help for list of commands and features");
        System.out.println("type to send message or use @user to pm someone");

        //initializing message handler threads
        cms = new Thread(new ClientMessageSender(this));
        cmr = new Thread(new ClientMessageReceiver(this));

        //starting message handler threads
        cms.start();
        cmr.start();



    }

    public void processCommand(Command c) throws Exception{
        String command = rsaUtil.decrypt(c.getCommand());
        String option = rsaUtil.decrypt(c.getOption());
        if(getRsaUtil().verifySignature(c.getCommand(),c.getSignature(),userMap.get("Server").getPublicKey())){
            switch (command) {
                case "removeuser":
                    removeUser(option);
                    break;
                default:
                    System.out.println("Command Error: Server attempted unrecognized command");
            }
        }
        else{
            System.out.println("Command Validation Error: command signature failed");
        }


    }

    public void sendPublicMessage(String message) throws Exception{
        for(int x = 0; x<userNameList.size();x++){
            Message m = buildEncryptedMessage(message,userNameList.get(x));
            output.writeObject(m);
        }
    }
    
    public void sendPrivateMessage(String message, String recipient) throws Exception{
        Message m = buildEncryptedMessage("~ " + message,recipient);
        output.writeObject(m);
    }

    public void decryptMessage(Message m) throws Exception{
        String sender = rsaUtil.decrypt(m.getSender());
        if(rsaUtil.verifySignature(m.getMessage(),m.getSignature(),userMap.get(sender).getPublicKey())){
            System.out.println(rsaUtil.decrypt(m.getSender()) + ": " + rsaUtil.decrypt(m.getMessage()));
        }
        else{
            System.out.println("Message Validation Error: message signature failed");
        }
    }

    public void sendServerCommand(String command, String option)throws Exception{
        Command c = buildEncryptedCommand(command, option);
        switch(command){
            case "quit":
                output.writeObject(c);
                closeConnection();
                break;
            case "users":
                for(int x = 0; x < userNameList.size(); x++){
                    if(userNameList.get(x).equals(userName)){
                        System.out.println(userNameList.get(x)+" <- you");
                    }
                    else if(!userNameList.get(x).equals("Server")){
                        System.out.println(userNameList.get(x));
                    }
                }
                break;
            case "help":
                break;
            default:
                System.out.println("command not recognized");
                break;

        }

    }

    public Object getObject() throws Exception{
        return input.readObject();
    }
    
    public Message buildEncryptedMessage(String message, String recipient) throws Exception{
        //creating encrypted message objects
        SealedObject messageEnc = rsaUtil.encrypt(message,userMap.get(recipient).getPublicKey());
        SealedObject signature = rsaUtil.sign(message);
        SealedObject senderEnc = rsaUtil.encrypt(userName, userMap.get(recipient).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get("Server").getPublicKey());
        
        return new Message(messageEnc,senderEnc,recipientEnc,signature);
    }

    public Command buildEncryptedCommand(String command,String option) throws Exception{

        SealedObject commandEnc = rsaUtil.encrypt(command,userMap.get("Server").getPublicKey());
        SealedObject signature = rsaUtil.sign(command);
        SealedObject senderEnc = rsaUtil.encrypt(userName, userMap.get("Server").getPublicKey());
        SealedObject optionEnc = rsaUtil.encrypt(option, userMap.get("Server").getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt("Server", userMap.get("Server").getPublicKey());

        return new Command(commandEnc,signature,optionEnc,senderEnc,recipientEnc);
    }
    
    public void closeConnection() throws IOException{
        online = false;
        s.close();
    }

    public boolean isOnline(){
        return online;
    }

    public void addUser(User u){
        if(!userNameList.contains(u.getUserName())) {
            userNameList.add(u.getUserName());
            userMap.put(u.getUserName(),u);
        }
    }
    public RSA getRsaUtil(){
        return rsaUtil;
    }
    public void removeUser(String name){
        userNameList.remove(name);
        userMap.remove(name);
    }

    public static void main(String[] args){
        try {
            Client c = new Client("localhost",5051);
        }
        catch(Exception e){
            System.out.println(e);
        }



    }

}
