import javax.crypto.SealedObject;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Server  {

    //hash maps which hold user sockets and stream information
    private Map<String, Socket> userSockets;
    private Map<String, ObjectInputStream> userInput;
    private Map<String, ObjectOutputStream> userOutput;
    private ArrayList<String> userNameList;
    private Map<String, User> userMap;
    private Crypto rsaUtil;
    private User serverUser;
    private String serverName;
    private DatabaseManager dbm;
    private ServerSocket ss;
    private boolean online;

    public Server(int port, String serverName,DatabaseManager dbm) throws Exception{
        //initializing user hash maps
        online = true;
        userSockets = new HashMap<>();
        userInput = new HashMap<>();
        userOutput = new HashMap<>();
        userNameList = new ArrayList<>();
        userMap = new HashMap<>();
        rsaUtil = new Crypto();
        rsaUtil.generateSharedKey();
        ss = new ServerSocket(port);
        this.serverName = serverName;
        serverUser = new User(serverName,rsaUtil.getPublicKey());
        this.dbm = dbm;

    }

    public Server(int port, String serverName, byte[] password, byte[] passwordSalt, byte[] serverAccessPassword, byte[] sapSalt, DatabaseManager dbm) throws Exception{
        userSockets = new HashMap<>();
        userInput = new HashMap<>();
        userOutput = new HashMap<>();
        userNameList = new ArrayList<>();
        userMap = new HashMap<>();
        rsaUtil = new Crypto();
        rsaUtil.generateSharedKey();
        ss = new ServerSocket(port);
        this.serverName = serverName;
        serverUser = new User(serverName,rsaUtil.getPublicKey());
        this.dbm = dbm;
        dbm.addNewServer(serverName,password,passwordSalt,serverAccessPassword,sapSalt);
        startServer();
    }


    public void acceptUser(Socket s) throws Exception{
        ObjectInputStream input;
        ObjectOutputStream output;
        String userName;
        String password;
        String serverAccessPassword;
        byte[] passwordHash;
        byte[] salt;
        byte[] ServerAccessPasswordSalt;
        Thread messageHandler;
        User tempUser;

        //1. creating input and output data streams
        output = new ObjectOutputStream(s.getOutputStream());
        input = new ObjectInputStream(s.getInputStream());

        //2. sending server's public key
        output.writeObject(serverUser);

        //3. getting register or sign in info from client
        boolean loginSuccess = false;
        boolean registerNewUser;

        while(!loginSuccess){
            registerNewUser = input.readBoolean();
            serverAccessPassword = rsaUtil.decrypt((SealedObject) input.readObject());
            ServerAccessPasswordSalt = dbm.getServerAccessSalt(serverName);
            userName = rsaUtil.decrypt((SealedObject) input.readObject());
            password = rsaUtil.decrypt((SealedObject) input.readObject());


            if(dbm.verifyServerAccessPassword(serverName,rsaUtil.hashPassword(serverAccessPassword,ServerAccessPasswordSalt,100000))) {

                if (isUserOnline(userName)) {
                    loginSuccess = false;
                    System.out.println("user " + userName + " tried to login while still online");

                } else if (registerNewUser) {

                    if (dbm.doesUserExist(serverName,userName)) {
                        loginSuccess = false;

                    } else {
                        salt = rsaUtil.generateSalt(32);
                        passwordHash = rsaUtil.hashPassword(password, salt, 200000);
                        dbm.addNewUser(serverName,userName, passwordHash, salt, 1);
                        loginSuccess = true;
                    }

                } else if (!dbm.doesUserExist(serverName,userName)) {
                    loginSuccess = false;

                } else {
                    passwordHash = rsaUtil.hashPassword(password, dbm.getUserSalt(serverName,userName, false), 200000);
                    loginSuccess = dbm.verifyPasswordHash(serverName,userName, passwordHash, false);

                }
            }

            if (loginSuccess) {
                output.writeObject("true");
            } else {
                output.writeObject("false");
            }



        }

        //4. getting clients public key and user info
        tempUser = (User) input.readObject();
        userName = tempUser.getUserName();
        userMap.put(userName, tempUser);
        userNameList.add(userName);

        //5 sending client shared key
        output.writeObject(rsaUtil.encryptKey(tempUser.getPublicKey()));


        //6. adding user input and output streams to maps
        userSockets.put(userName,s);
        userInput.put(userName,input);
        userOutput.put(userName,output);

        //7. start message handler
        messageHandler = new Thread(new ServerMessageHandler(this,userName));
        messageHandler.start();

        //8. sending keys to users
        sendUsers(userName);

        //9.displaying newly added user
        System.out.println("user " + userName + " has been added to" + serverName);
        sendMessageAll(userName + " has joined the server");

        sendMessage("welcome to the server " + userName, userName);
    }

    public void startServer() throws Exception{
        Socket s;
        Thread ua;

        while(online){
            s = ss.accept();
            ua = new Thread(new ServerUserAcceptor(this,s));
            ua.start();
        }
    }

    //gets message or command object from a specific senders input stream
    public Object getObject(String Sender) throws Exception {
        return userInput.get(Sender).readObject();
    }

    //removes all userSockets references from maps
    public void removeUser(String userName) throws Exception{
        userSockets.remove(userName);
        userInput.get(userName).close();
        userInput.get(userName).close();
        userInput.remove(userName);
        userOutput.remove(userName);
        userMap.remove(userName);
        for(int i = 0; i<userNameList.size(); i++){
            if(userName.equals(userNameList.get(i))){
                userNameList.remove(i);
            }
        }
        sendCommandAll("removeuser",userName);
        sendMessageAll(userName + " has left");
    }


    public void commandProcessor(Command c, String senderName) throws Exception{
        //String command = rsaUtil.decrypt(c.getCommand());
        //String argument = rsaUtil.decrypt(c.getArgument1());
        String command = rsaUtil.decryptAES(c.getCommand(),c.getIvParam());
        String argument1 = rsaUtil.decryptAES(c.getArgument1(),c.getIvParam());
        String argument2 = rsaUtil.decryptAES(c.getArgument2(),c.getIvParam());

        //new server commands can be added below to switch statement
        //if(rsaUtil.verifySignature(c.getCommand(),c.getSignature(),userMap.get(senderName).getPublicKey()))
        if(rsaUtil.verifyCommandSignature(c,userMap.get(senderName).getPublicKey())){
            switch (command){
                case "quit":
                    removeUser(senderName);
                    break;
                default:
                    System.out.println("user " + senderName + "requested unknown command " + command);
            }
        }
        else{
            System.out.println("Command Validation Error: command signature failed");
        }

    }

    //checks if the user is currently online
    public boolean isUserOnline(String userName){
        for(int i = 0; i<userNameList.size(); i++){
            if(userName.equals(userNameList.get(i))){
                return true;
            }
        }
        return false;
    }

    //Sends the servers user object to a single client
    public void sendServerUser(String recipient) throws Exception{
        userOutput.get(recipient).writeObject(serverUser);
    }
    
    //sends all clients the newly created clients user object
    public void sendUsers(String newUser) throws Exception{
        for(int x = 0; x < userNameList.size(); x++){
            userOutput.get(userNameList.get(x)).writeObject(userMap.get(newUser));
        }
        for(int x = 0; x < userNameList.size(); x++) {
            if(!userNameList.get(x).equals(newUser)){
                userOutput.get(newUser).writeObject(userMap.get(userNameList.get(x)));
            }
        }
    }
    
    //Sends a server command to a single recipient
    public void sendCommand(String command, String argument, String recipient)throws Exception{
        //userOutput.get(recipient).writeObject(buildEncryptedCommand(command,argument,recipient));
        userOutput.get(recipient).writeObject(rsaUtil.encryptCommandAES(command,argument,"none",serverName,recipient));
    }
    
    //Sends a server command to all clients on server
    public void sendCommandAll(String command, String option) throws Exception{
        if(isServerOnline()) {
            for (int x = 0; x < userNameList.size(); x++) {
                sendCommand(command, option, userNameList.get(x));
            }
        }
    }
    
    //Sends a message from the server to all clients
    public void sendMessageAll(String message) throws Exception{
        if(isServerOnline()) {
            for (int x = 0; x < userNameList.size(); x++) {
                sendMessage(message, userNameList.get(x));
            }
        }
    }

    //Sends a single message from the server to a single recipient
    public void sendMessage(String message, String recipient) throws Exception{
        //userOutput.get(recipient).writeObject(buildEncryptedMessage(message,recipient));
        userOutput.get(recipient).writeObject(rsaUtil.encryptMessageAES(message,serverName,recipient));
    }
    
    //relays a message from the sender to the recipient without looking at the contents of the message
    public void relayMessage(Message m) throws Exception{
        userOutput.get(rsaUtil.decryptAES(m.getRecipient(),m.getIv())).writeObject(m);
    }


    public boolean closeConnection(){
        boolean closeSuccess = false;
        try{
            online = false;
            ss.close();
            closeSuccess = true;

        }
        catch (Exception e){
            System.out.println("Failed to close server connection: " + e);
        }
        return closeSuccess;

    }

    public int getUserPrivilege(String username) throws Exception{
        return dbm.getUserPrivilege(serverName,username);
    }

    public ArrayList<String> getAllUsers(){
        return dbm.getUserList(serverName);
    }

    public void setUserPrivilege(String username, int privilege) throws Exception{

        String message1 = "";
        String message2 = "";

        dbm.setUserPrivilege(serverName,username,privilege);

        switch (privilege){
            case 0:
                message1 = "You have been muted by the server and will no longer be able to send messages";
                message2 = "User " + username + " has been muted by the server";
                break;
            case 1:
                message1 = "Your privilege has been changed to User (1)";
                message2 = "User " + username + " is now an User";
                break;
            case 2:
                message1 = "Your privilege has been changed to  Admin (2)";
                message2 = "User " + username + " is now an Admin";
                break;
            case 3:
                message1 = "Your privilege has been changed to Owner (3)";
                message2 = "User " + username + " is now an Owner";
                break;

        }
        if(isUserOnline(username)){
            sendMessage(message1,username);
        }
        sendMessageAll(message2);
    }

    public boolean isServerOnline(){
        return online;
    }

    public String getServerName(){
        return serverName;
    }


}
