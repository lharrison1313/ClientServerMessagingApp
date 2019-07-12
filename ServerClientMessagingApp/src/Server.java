import javax.crypto.SealedObject;
import java.net.*;
import java.io.*;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Server  {

    //hash maps which hold user sockets and stream information
    private Map<String, Socket> users;
    private Map<String, ObjectInputStream> userInput;
    private Map<String, ObjectOutputStream> userOutput;
    private ArrayList<String> userNameList;
    private Map<String, User> userMap;
    private RSA rsaUtil;
    private User serverUser;



    public Server(int port) throws Exception{

        //initializing user hash maps
        users = new HashMap<String,Socket>();
        userInput = new HashMap<String,ObjectInputStream>();
        userOutput = new HashMap<String,ObjectOutputStream>();
        userNameList = new ArrayList<String>();
        userMap = new HashMap<>();
        rsaUtil = new RSA();
        serverUser = new User("Server",rsaUtil.getPublicKey());


        //declaring socket and stream variables
        ServerSocket ss;
        Socket s;
        ObjectInputStream input;
        ObjectOutputStream output;
        String userName;
        Thread messageHandler;
        ss = new ServerSocket(port);

        while(true){
            s = ss.accept();

            //creating input and output data streams
            output = new ObjectOutputStream(s.getOutputStream());
            input = new ObjectInputStream(s.getInputStream());

            //getting username from client and checking if it already exists
            boolean usernameSuccess;
            do{
                userName = (String) input.readObject();

                if(userName.isEmpty() || userName.equals(" ") || userName.startsWith(" ") || userName.equals("Server")){
                    usernameSuccess = false;
                    output.writeObject("false");
                }
                else if(isUserOnline(userName)){
                    output.writeObject("false");
                    usernameSuccess = false;
                }
                else{
                    usernameSuccess = true;
                }
            }while(!usernameSuccess);
            output.writeObject("true");
            userNameList.add(userName);

            //getting public key for user
            userMap.put(userName,(User) input.readObject());
            System.out.println("got public key");

            //placing user sockets and object streams in hash maps
            users.put(userName, s);
            userInput.put(userName, input);
            userOutput.put(userName, output);

            //creating message handler thread for user
            messageHandler = new Thread(new ServerMessageHandler(this,userName,rsaUtil));
            messageHandler.start();

            //displaying newly added user
            System.out.println("user " + userName + " has been added to the server");
            //relayMessage(rsaUtil.encrypt("welcome to the server " + userName, userPublicKeyList.get(userName)), userName);
            sendMessageAll(userName + " has joined the server");

            //Sends keys to all users
            sendUsers(userName);
            sendServerUser(userName);
        }

    }

    //gets message or command object from a specific senders input stream
    public Object getObject(String Sender) throws Exception{
        return userInput.get(Sender).readObject();
    }

    //removes all users references from maps
    public void removeUser(String userName) throws Exception{
        users.remove(userName);
        userInput.remove(userName);
        userOutput.remove(userName);
        userMap.remove(userName);
        for(int i = 0; i<userNameList.size(); i++){
            if(userName.equals(userNameList.get(i))){
                userNameList.remove(i);
            }
        }
        sendCommandAll("removeuser",userName);
        sendMessageAll(userName + " has Left");
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
        SealedObject senderEnc = rsaUtil.encrypt("Server", userMap.get(recipient).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get(recipient).getPublicKey());

        return new Message(messageEnc,senderEnc,recipientEnc);
    }

    //builds an encrypted command object given the command an option and a recipient
    public Command buildEncryptedCommand(String command, String option, String recipient) throws Exception{
        SealedObject commandEnc = rsaUtil.encrypt(command,userMap.get(recipient).getPublicKey());
        SealedObject optionEnc = rsaUtil.encrypt(option, userMap.get(recipient).getPublicKey());
        SealedObject senderEnc = rsaUtil.encrypt("server", userMap.get(recipient).getPublicKey());
        SealedObject recipientEnc = rsaUtil.encrypt(recipient, userMap.get(recipient).getPublicKey());

        return new Command(commandEnc,optionEnc,senderEnc,recipientEnc);
    }

    //returns the username list
    public ArrayList<String> getUserNameList(){
        return userNameList;
    }

    public static void main(String[] args){
        try{
            Server serve = new Server(5051);
        }
        catch (Exception e){
            System.out.println(e);
        }

    }


}
