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
    private String serverName;
    private String userName;
    private boolean online;
    private Thread cmr;
    private Map<String, User> userMap;
    private ArrayList<String> userNameList;
    private RSA rsaUtil;
    private MessagingGUI messagingWindow;
    private String host;
    private int port;


    public Client(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        rsaUtil = new RSA();
        userMap = new HashMap<>();
        userNameList = new ArrayList<>();
        connectToServerP1();

    }

    private void connectToServerP1() throws Exception{
        online = true;
        User serverUser;

        //1. connecting to server
        s = new Socket(host,port);

        //2. creating input and output data streams
        input = new ObjectInputStream(s.getInputStream());
        output = new ObjectOutputStream(s.getOutputStream());

        //3. getting server's user object
        serverUser = (User) input.readObject();
        addUser(serverUser);
        serverName = serverUser.getUserName();


    }

    public boolean sendLogInInfo(boolean newUser, String serverAccessPassword, String username, String password )throws Exception{
        boolean loginSuccess;

        //sending login info to server
        output.writeBoolean(newUser);
        output.writeObject(rsaUtil.encrypt(serverAccessPassword, userMap.get(serverName).getPublicKey()));
        output.writeObject(rsaUtil.encrypt(username, userMap.get(serverName).getPublicKey()));
        output.writeObject(rsaUtil.encrypt(password, userMap.get(serverName).getPublicKey()));

        //getting login success response from server
        String response = (String) input.readObject();

        if(response.equals("true")){
            connectToServerP2();
            loginSuccess = true;
        }
        else{
            loginSuccess = false;
        }

        return loginSuccess;
    }

    private void connectToServerP2()throws Exception{
        //4. sending clients user info
        User clientUser = new User(userName,rsaUtil.getPublicKey());
        userNameList.add(userName);
        userMap.put(userName,clientUser);
        output.writeObject(clientUser);

        //5. setting up gui and displaying startup info
        messagingWindow = new MessagingGUI(this);
        messagingWindow.appendTextArea("type $help for list of commands and features");
        messagingWindow.appendTextArea("type to send message or use @user to pm someone");

        //6. setting up client message sender and receiver
        cmr = new Thread(new ClientMessageReceiver(this));
        cmr.start();
    }

    public boolean sendMessage(String message){
        try{
            if(message.substring(0,1).equals("@") && message.length() >1 && message.contains(" ")){
                String[] messageList = message.split(" ", 2);

                String receiver = messageList[0].substring(1);
                message = messageList[1];
                sendPrivateMessage(message,receiver);
                return false;
            }
            else if(message.substring(0,1).equals("$") && message.length() >1 && !message.contains(" ")){
                if(message.substring(1).equals("quit")){
                    sendServerCommand("quit","none");
                    return true;
                }
                else {
                    sendServerCommand(message.substring(1), "none");
                    return false;
                }
            }
            else{
                sendPublicMessage(message);
                return false;
            }

        }
        catch (Exception e){
            System.out.println("Client Message Sender Error: " + e);
            return false;
        }
    }

    public void processCommand(Command c) throws Exception{
        String command = rsaUtil.decrypt(c.getCommand());
        String option = rsaUtil.decrypt(c.getOption());
        if(getRsaUtil().verifySignature(c.getCommand(),c.getSignature(),userMap.get(serverName).getPublicKey())){
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
            if(!userNameList.get(x).equals(serverName)) {
                Message m = buildEncryptedMessage(message, userNameList.get(x));
                output.writeObject(m);
            }
        }
    }

    public void sendPrivateMessage(String message, String recipient) throws Exception{
        Message m = buildEncryptedMessage(String.format("~ %s", message),recipient);
        output.writeObject(m);
        if(!recipient.equals(userName)){
            messagingWindow.appendTextArea( String.format("%s: ~ %s",userName, message));
        }
    }

    public void decryptMessage(Message m) throws Exception{
        String sender = rsaUtil.decrypt(m.getSender());
        String message = rsaUtil.decrypt(m.getMessage());
        if(rsaUtil.verifySignature(m.getMessage(),m.getSignature(),userMap.get(sender).getPublicKey() )){
                messagingWindow.appendTextArea(String.format("%s: %s", sender,message));

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
                messagingWindow.appendTextArea("Users Currently Online:");
                for(int x = 0; x < userNameList.size(); x++){
                    if(userNameList.get(x).equals(userName)){
                        messagingWindow.appendTextArea(String.format("%s <- you", userName));
                    }
                    else if(!userNameList.get(x).equals(serverName)){
                        messagingWindow.appendTextArea(userNameList.get(x));
                    }
                }
                break;
            case "help":
                break;
            default:
                messagingWindow.appendTextArea("command not recognized");
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
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get(serverName).getPublicKey());
        
        return new Message(messageEnc,senderEnc,recipientEnc,signature);
    }

    public Command buildEncryptedCommand(String command,String option) throws Exception{

        SealedObject commandEnc = rsaUtil.encrypt(command,userMap.get(serverName).getPublicKey());
        SealedObject signature = rsaUtil.sign(command);
        SealedObject senderEnc = rsaUtil.encrypt(userName, userMap.get(serverName).getPublicKey());
        SealedObject optionEnc = rsaUtil.encrypt(option, userMap.get(serverName).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(serverName, userMap.get(serverName).getPublicKey());

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

    public void setUserName(String userName){
        this.userName = userName;
    }

}
