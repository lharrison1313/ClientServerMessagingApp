import javax.crypto.SealedObject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
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

    }

    public void connectToServer() throws Exception{
        Scanner scan = new Scanner(System.in);
        online = true;
        User serverUser;
        User clientUser;


        //1. connecting to server
        s = new Socket(host,port);
        System.out.println("1");

        //2. creating input and output data streams
        input = new ObjectInputStream(s.getInputStream());
        output = new ObjectOutputStream(s.getOutputStream());
        System.out.println("2");

        //3. getting server's user object
        serverUser = (User) input.readObject();
        addUser(serverUser);
        serverName = serverUser.getUserName();
        System.out.println("3");

        //4. verify servers access password
        System.out.println("4");

        //5. sending clients sign in and register info to server
        boolean loginSuccess = false;
        boolean registerNewUser;
        String response;
        String password;

        while(!loginSuccess){

            System.out.println("register or sign in");
            response = scan.nextLine();

            if(response.equals("register")) {
                registerNewUser = true;
            }
            else{
                registerNewUser = false;
            }

            System.out.println("enter username");
            userName = scan.nextLine();
            System.out.println("enter password");
            password = scan.nextLine();

            output.writeBoolean(registerNewUser);
            output.writeObject(rsaUtil.encrypt(userName,serverUser.getPublicKey()));
            output.writeObject(rsaUtil.encrypt(password,serverUser.getPublicKey()));

            response = (String) input.readObject();

            if(response.equals("true")){
                loginSuccess = true;
            }

        }
        System.out.println("5");

        //6. sending clients user info
        clientUser = new User(userName,rsaUtil.getPublicKey());
        userNameList.add(userName);
        userMap.put(userName,clientUser);
        output.writeObject(clientUser);
        System.out.println("6");

        //7. setting up gui and displaying startup info
        messagingWindow = new MessagingGUI();
        messagingWindow.appendTextArea("type $help for list of commands and features");
        messagingWindow.appendTextArea("type to send message or use @user to pm someone");
        System.out.println("7");

        //8. setting up client message sender and reciever
        cmr = new Thread(new ClientMessageReceiver(this));
        createClientMessageSender();
        cmr.start();
        createCloseActionListener();
        System.out.println("8");




    }

    public void createCloseActionListener(){
        messagingWindow.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try{
                    sendServerCommand("quit","none");
                }
                catch (Exception e1){
                    System.out.println("error sending server quit command");
                }
                e.getWindow().dispose();
            }
        });
    }

    public void createClientMessageSender() throws Exception{
        messagingWindow.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try{
                    //gets input from user and checks for quit command
                    String userInput = messagingWindow.getTextField().getText();
                    messagingWindow.getTextField().setText("");
                    if(userInput.substring(0,1).equals("@") && userInput.length() >1 && userInput.contains(" ")){
                        String[] messageList = userInput.split(" ", 2);

                        String receiver = messageList[0].substring(1);
                        userInput = messageList[1];
                        sendPrivateMessage(userInput,receiver);
                    }
                    else if(userInput.substring(0,1).equals("$") && userInput.length() >1 && !userInput.contains(" ")){
                        if(userInput.substring(1).equals("quit")){
                            messagingWindow.getFrame().dispatchEvent(new WindowEvent(messagingWindow.getFrame(),WindowEvent.WINDOW_CLOSING));
                        }
                        else {
                            sendServerCommand(userInput.substring(1), "none");
                        }
                    }
                    else{
                        sendPublicMessage(userInput);
                    }

                }
                catch (Exception e2){
                    System.out.println("Client Message Sender Error: " + e2);
                }
            }
        });
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

    public static void main(String[] args){
        try {
            Client c = new Client("localhost",5051);
            c.connectToServer();
        }
        catch(Exception e){
            System.out.println(e);
        }



    }

}
