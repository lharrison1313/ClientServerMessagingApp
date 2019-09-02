import javax.crypto.SealedObject;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

 /*
    Purpose: This class contains all necessary methods for communication between
    a server and other clients. In order for the client to successfully connect
    with the server, an instance of the server class must be running and the
    sendLogInInfo method must be called.

 */

public class Client {

    //declaring socket, stream variables, and client private field
    private Socket s;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String serverName;
    private String userName;
    private boolean online;
    private Thread cmr;
    private Map<String, User> userMap;  //contains all of the currently online users and their respective User Objects
    private ArrayList<String> userNameList; //contains all of the usernames of currently online users
    private Crypto cryptoUtil;
    private String host;
    private int port;
    private BlockingQueue<String> messageQueue; //contains all of the incoming messages from the server and other clients on the server


    public Client(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        cryptoUtil = new Crypto();
        userMap = new HashMap<>();
        userNameList = new ArrayList<>();
        messageQueue = new LinkedBlockingQueue<>();
        connectToServerP1();

    }

    /*
        Establishing Server Connection: In order to establish the connection
        between the server and client the client must carry out the following
        procedures in this order; connectToServerP1, sendLogInInfo, connectToServerP2.
        The only method that must be called explicitly within the program is sendLogInInfo.
    */

    //connectToServerP1: This method is the first procedure in establishing a connection with the server
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

    /*
        sendLogInInfo: this method is the second procedure used in establishing a connection with the server
        and is used to send login or registration info to the server.

        @param newUser: if true a new account will be added to the servers database, if false the user will sign in
        with an existing account.
        @param serverAccessPassword: this is the password that each user uses to access a specific server.
        @param username: this is the username for the users account.
        @param password: this is the unique password a user uses to access their account.

        @return if the login is successful then the method will return true.
     */
    public boolean sendLogInInfo(boolean newUser, String serverAccessPassword, String username, String password )throws Exception{
        boolean loginSuccess;

        //sending login info to server
        output.writeBoolean(newUser);
        output.writeObject(cryptoUtil.encrypt(serverAccessPassword, userMap.get(serverName).getPublicKey()));
        output.writeObject(cryptoUtil.encrypt(username, userMap.get(serverName).getPublicKey()));
        output.writeObject(cryptoUtil.encrypt(password, userMap.get(serverName).getPublicKey()));

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

    // This method is the final procedure in establishing a connection with the server
    private void connectToServerP2()throws Exception{
        //4. sending clients user info
        User clientUser = new User(userName, cryptoUtil.getPublicKey());
        userNameList.add(userName);
        userMap.put(userName,clientUser);
        output.writeObject(clientUser);

        //4.5 getting shared key
        cryptoUtil.setSharedKey(cryptoUtil.decryptKey((SealedObject) input.readObject()));

        //5. displaying startup info
        messageQueue.add("type $help for list of commands and features");
        messageQueue.add("type to send message or use @user to pm someone");

        //6. setting up client message receiver
        cmr = new Thread(new ClientMessageReceiver(this));
        cmr.start();
    }

    /*
        sendMessage: this method processes a string and sends it to the server as either a
        command, private message, or public message.

        @param message: this is the string that will be processed and sent to the server

        @return the method will return a string that will identify the type of message sent
        privateM - private message to a single user connected to server
        quit - a command to disconnect from the server
        command - a general command sent to the server
        publicM - public message sent to all users connected to server
        disconnect - shows the server is no longer online
    */

    public String sendMessage(String message){
        String response;
        if(online) {
            try {
                //checking if message is a private message
                if (message.substring(0, 1).equals("@") && message.length() > 1 && message.contains(" ")) {
                    String[] messageList = message.split(" ", 2);

                    String receiver = messageList[0].substring(1);
                    message = messageList[1];
                    sendPrivateMessage(message, receiver);
                    response = "privateM";
                }
                //checking if message is a command
                else if (message.substring(0, 1).equals("$") && message.length() > 1 && !message.contains(" ")) {
                    if (message.substring(1).equals("quit")) {
                        sendServerCommand("quit", "none");
                        response = "quit";
                    } else {
                        sendServerCommand(message.substring(1), "none");
                        response = "command";
                    }
                }
                // message is a public message
                else {
                    sendPublicMessage(message);
                    response = "publicM";
                }

            } catch (Exception e) {
                System.out.println("Client Message Sender Error: " + e);
                response = "disconnect";
            }
        }
        else{
            response = "disconnect";
        }
        return  response;
    }

    /*
        processCommand: this method processes a command sent by the server and received by the client

        @param a command object which holds the command sent by the server

     */
    public void processCommand(Command c) throws Exception{
        //decrypting command and command parameter
        //String command = cryptoUtil.decryptAES(c.getCommand());
        //String argument = cryptoUtil.decryptAES(c.getArgument1());
        String command = cryptoUtil.decryptAES(c.getCommand(),c.getIvParam());
        String argument1 = cryptoUtil.decryptAES(c.getArgument1(),c.getIvParam());
        String argumetn2 = cryptoUtil.decryptAES(c.getArgument2(),c.getIvParam());

        //verifying commands signature
        //if(cryptoUtil.verifySignature(c.getCommand(),c.getSignature(),userMap.get(serverName).getPublicKey()))
        if(cryptoUtil.verifyCommandSignature(c,userMap.get(serverName).getPublicKey())){

            //checking which command was sent
            switch (command) {
                case "removeuser":
                    removeUser(argument1);
                    break;
                default:
                    System.out.println("Command Error: Server attempted unrecognized command");
                    break;
            }
        }
        else{
            System.out.println("Command Validation Error: command signature failed");
        }


    }

    /*
        sendPublicMessage: sends a message to all users on the server

        @param message: the message to be sent to users on the server
     */
    public void sendPublicMessage(String message) throws Exception{
        for(int x = 0; x<userNameList.size();x++){
            if(!userNameList.get(x).equals(serverName)) {
                //Message m = buildEncryptedMessage(message, userNameList.get(x));
                Message m = cryptoUtil.encryptMessageAES(message,userName,userNameList.get(x));
                output.writeObject(m);
            }
        }
    }

    /*
        sendPrivateMessage: sends a message to a single user on the server

        @param message: the message to be sent to the user on the server
        @param recipient: the user who will receive the message
     */
    public void sendPrivateMessage(String message, String recipient) throws Exception{
        //Message m = buildEncryptedMessage(String.format("~ %s", message),recipient);
        Message m = cryptoUtil.encryptMessageAES(String.format("~ %s", message),userName,recipient);
        output.writeObject(m);
        if(!recipient.equals(userName)){
            messageQueue.add(String.format("%s: ~ %s",userName, message));
        }
    }

    /*
        decryptMessage: decrypts a message and adds it to the message queue

        @param m: a message object containing the encrypted message and sender
     */
    public void decryptMessage(Message m) throws Exception{
        //String sender = cryptoUtil.decrypt(m.getSender());
        //String message = cryptoUtil.decrypt(m.getMessage());
        String sender = cryptoUtil.decryptAES(m.getSender(),m.getIv());
        String message = cryptoUtil.decryptAES(m.getMessage(),m.getIv());

        // if(cryptoUtil.verifySignature(m.getMessage(),m.getSignature(),userMap.get(sender).getPublicKey() ))
        if(cryptoUtil.verifyMessageSignature(m,userMap.get(sender).getPublicKey())){
                messageQueue.add(String.format("%s: %s", sender,message));
        }
        else{
            System.out.println("Message Validation Error: message signature failed");
        }
    }

    /*
        sendServerCommand: sends a command to the server unless the client can handle the request. Some examples of commands which
        clients can handle are list users, and help

        @param command: the command the client is attempting to send to the server
        @param argument: an argument for the command
     */
    public void sendServerCommand(String command, String argument)throws Exception{
        //Command c = buildEncryptedCommand(command, argument);
        Command c = cryptoUtil.encryptCommandAES(command,argument,"none",userName,serverName);
        switch(command){
            case "quit":
                //closes the connection between the client and server
                output.writeObject(c);
                closeConnection();
                break;
            case "users":
                //lists all the currently online users
                messageQueue.add("Users Currently Online:");
                for(int x = 0; x < userNameList.size(); x++){
                    if(userNameList.get(x).equals(userName)){
                        messageQueue.add(String.format("%s <- you", userName));
                    }
                    else if(!userNameList.get(x).equals(serverName)){
                        messageQueue.add(userNameList.get(x));
                    }
                }
                break;
            case "help":
                //lists a list of commands
                break;
            default:
                //unrecognized command
                messageQueue.add("command not recognized");
                break;

        }

    }

    // This method gets an object from the object input stream
    public Object getObject() throws Exception{
        return input.readObject();
    }

    //closes connection between server and client
    public void closeConnection(){
        try{
            online = false;
            input.close();
            output.close();
            s.close();
        }
        catch (Exception e){
            System.err.println("Error when closing connections: " + e);
        }

    }

    // checks if the client is currently online
    public boolean isOnline(){
        return online;
    }

    /*
        addUser: adds a new user to the servers userNameList and userMap

        @param u: a user object containing the username and rsa public key

     */
    public void addUser(User u){
        if(!userNameList.contains(u.getUserName())) {
            userNameList.add(u.getUserName());
            userMap.put(u.getUserName(),u);
        }
    }

    /*
        removeUser: removes a user from the servers user list

        @param name: the name of the user to be removed
     */
    public void removeUser(String name){
        userNameList.remove(name);
        userMap.remove(name);
    }

    //returns the Queue object that holds all incoming messages from the server and other clients
    public BlockingQueue<String> getMessageQueue(){
        return messageQueue;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

}
