import javax.crypto.SealedObject;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class Server  {

    //hash maps which hold user sockets and stream information
    private Map<String, Socket> userSockets;
    private Map<String, ObjectInputStream> userInput;
    private Map<String, ObjectOutputStream> userOutput;
    private ArrayList<String> userNameList;
    private Map<String, User> userMap;
    private RSA rsaUtil;
    private User serverUser;
    private String servername;
    private DatabaseManager dbm;
    private ServerSocket ss;

    public Server(int port) throws Exception{

        //initializing user hash maps
        userSockets = new HashMap<>();
        userInput = new HashMap<>();
        userOutput = new HashMap<>();
        userNameList = new ArrayList<>();
        userMap = new HashMap<>();
        rsaUtil = new RSA();
        ss = new ServerSocket(port);

    }

    public void initializeServer() throws Exception{
        Scanner scan = new Scanner(System.in);
        String response1;
        String response2;
        String sap;
        String password;
        byte[] salt1, salt2;
        byte[] hash1, hash2;


        System.out.println("build new Database");
        response1 = scan.nextLine();
        System.out.println("register a new server or sign in to existing server");
        response2 = scan.nextLine();
        System.out.println("enter server name");
        servername = scan.nextLine();
        System.out.println("enter password");
        password = scan.nextLine();

        if(response2.equals("register") && response1.equals("yes")){
            System.out.println("enter server access password");
            sap = scan.nextLine();
            salt1 = RSA.generateSalt(32);
            salt2 = RSA.generateSalt(32);
            hash1 = RSA.hashPassword(password,salt1,100000);
            hash2 = RSA.hashPassword(sap, salt2, 100000);
            dbm = new DatabaseManager(servername,hash1,salt1, hash2, salt2,true);
        }
        else if(response2.equals("register")){
            System.out.println("enter server access password");
            sap = scan.nextLine();
            salt1 = RSA.generateSalt(32);
            salt2 = RSA.generateSalt(32);
            hash1 = RSA.hashPassword(password,salt1,100000);
            hash2 = RSA.hashPassword(sap,salt2,100000);
            dbm = new DatabaseManager(servername,hash1,salt1,hash2,salt2,false);
        }
        else{
            dbm = new DatabaseManager(servername);
            salt1 = dbm.getUserSalt(servername,true);
            hash1 = RSA.hashPassword(password,salt1,100000);
            System.out.println(dbm.verifyPasswordHash(servername,hash1,true));
        }

        serverUser = new User(servername,rsaUtil.getPublicKey());

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


        //1. acceptingClient
        System.out.println("1");

        //2. creating input and output data streams
        output = new ObjectOutputStream(s.getOutputStream());
        input = new ObjectInputStream(s.getInputStream());
        System.out.println("2");

        //3. sending server's public key
        output.writeObject(serverUser);
        System.out.println("3");

        //4. verify sever access password
        System.out.println("4");

        //5. getting register or sign in info from client
        boolean loginSuccess = false;
        boolean registerNewUser;

        while(!loginSuccess){
            registerNewUser = input.readBoolean();
            serverAccessPassword = rsaUtil.decrypt((SealedObject) input.readObject());
            ServerAccessPasswordSalt = dbm.getServerAccessSalt(servername);
            userName = rsaUtil.decrypt((SealedObject) input.readObject());
            password = rsaUtil.decrypt((SealedObject) input.readObject());


            if(dbm.verifyServerAccessPassword(servername,rsaUtil.hashPassword(serverAccessPassword,ServerAccessPasswordSalt,100000))) {

                if (isUserOnline(userName)) {
                    loginSuccess = false;
                    System.out.println("user " + userName + " tried to login while still online");

                } else if (registerNewUser) {

                    if (dbm.doesUserExist(userName)) {
                        loginSuccess = false;

                    } else {
                        salt = rsaUtil.generateSalt(32);
                        passwordHash = rsaUtil.hashPassword(password, salt, 200000);
                        dbm.addNewUser(userName, passwordHash, salt, 1);
                        loginSuccess = true;
                    }

                } else if (!dbm.doesUserExist(userName)) {
                    loginSuccess = false;

                } else {
                    passwordHash = rsaUtil.hashPassword(password, dbm.getUserSalt(userName, false), 200000);
                    loginSuccess = dbm.verifyPasswordHash(userName, passwordHash, false);

                }
            }

            if (loginSuccess) {
                output.writeObject("true");
            } else {
                output.writeObject("false");
            }



        }
        System.out.println("5");

        //6. getting clients public key and user info
        tempUser = (User) input.readObject();
        userName = tempUser.getUserName();
        userMap.put(userName, tempUser);
        userNameList.add(userName);
        System.out.println("6");

        //7. adding user input and output streams to maps
        userSockets.put(userName,s);
        userInput.put(userName,input);
        userOutput.put(userName,output);
        System.out.println("7");

        //8. start message handler
        messageHandler = new Thread(new ServerMessageHandler(this,userName,rsaUtil));
        messageHandler.start();
        System.out.println("8");

        //9. sending keys to users
        sendUsers(userName);
        System.out.println("9");

        //10.displaying newly added user
        System.out.println("user " + userName + " has been added to the server");
        sendMessageAll(userName + " has joined the server");

        sendMessage("welcome to the server " + userName, userName);
        System.out.println("10");
    }

    public void startServer() throws Exception{
        Socket s;
        Thread ua;

        while(true){
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
        String command = rsaUtil.decrypt(c.getCommand());
        String option = rsaUtil.decrypt(c.getOption());

        //new server commands can be added below to switch statement
        if(rsaUtil.verifySignature(c.getCommand(),c.getSignature(),userMap.get(senderName).getPublicKey())){
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
    public void sendCommand(String command, String option, String recipient)throws Exception{
        userOutput.get(recipient).writeObject(buildEncryptedCommand(command,option,recipient));
    }
    
    //Sends a server command to all clients on server
    public void sendCommandAll(String command, String option) throws Exception{
        for(int x = 0; x < userNameList.size(); x++){
            sendCommand(command,option,userNameList.get(x));
        }
    }
    
    //Sends a message from the server to all clients
    public void sendMessageAll(String message) throws Exception{
        for(int x = 0; x < userNameList.size(); x++){
            sendMessage(message,userNameList.get(x));
        }
    }

    //Sends a single message from the server to a single recipient
    public void sendMessage(String message, String recipient) throws Exception{
        userOutput.get(recipient).writeObject(buildEncryptedMessage(message,recipient));
    }
    
    //relays a message from the sender to the recipient without looking at the contents of the message
    public void relayMessage(Message m) throws Exception{
        userOutput.get(rsaUtil.decrypt(m.getRecipient())).writeObject(m);
    }

    //builds an encrypted message object given strings of the message body and recipient
    public Message buildEncryptedMessage(String message, String recipient) throws Exception{
        //creating encrypted message objects
        SealedObject messageEnc = rsaUtil.encrypt(message,userMap.get(recipient).getPublicKey());
        SealedObject signature = rsaUtil.sign(message);
        SealedObject senderEnc = rsaUtil.encrypt(serverUser.getUserName(), userMap.get(recipient).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get(recipient).getPublicKey());

        return new Message(messageEnc,senderEnc,recipientEnc,signature);
    }

    //builds an encrypted command object given the command an option and a recipient
    public Command buildEncryptedCommand(String command, String option, String recipient) throws Exception{
        SealedObject commandEnc = rsaUtil.encrypt(command,userMap.get(recipient).getPublicKey());
        SealedObject signature = rsaUtil.sign(command);
        SealedObject optionEnc = rsaUtil.encrypt(option, userMap.get(recipient).getPublicKey());
        SealedObject senderEnc = rsaUtil.encrypt(serverUser.getUserName(), userMap.get(recipient).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get(recipient).getPublicKey());

        return new Command(commandEnc,signature,optionEnc,senderEnc,recipientEnc);
    }

    //returns the username list
    public ArrayList<String> getUserNameList(){
        return userNameList;
    }

    public static void main(String[] args){
        try{
            Server serve = new Server(5051);
            serve.initializeServer();
            serve.startServer();

        }
        catch (Exception e){
            System.out.println(e);
        }

    }


}
