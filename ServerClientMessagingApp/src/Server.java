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
    private Map<String, PublicKey> userPublicKeyList;
    private RSA rsaUtil;



    public Server(int port) throws Exception{

        //initializing user hash maps
        users = new HashMap<String,Socket>();
        userInput = new HashMap<String,ObjectInputStream>();
        userOutput = new HashMap<String,ObjectOutputStream>();
        userNameList = new ArrayList<String>();
        userPublicKeyList = new HashMap<String,PublicKey>();
        rsaUtil = new RSA();


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
            userPublicKeyList.put(userName,(PublicKey) input.readObject());
            System.out.println("got public key");

            //placing user sockets and datastreams in hash maps
            users.put(userName, s);
            userInput.put(userName, input);
            userOutput.put(userName, output);

            //creating message handler thread for user
            messageHandler = new Thread(new ServerMessageHandler(this,userName,rsaUtil));
            messageHandler.start();

            //displaying newly added user
            System.out.println("user " + userName + " has been added to the server");
            //sendMessage(rsaUtil.encrypt("welcome to the server " + userName, userPublicKeyList.get(userName)), userName);
            sendMessageAll(userName + " has joined the server");

            //Sends keys to all users
            sendKeys(userName, userPublicKeyList.get(userName));
            sendServerKey(userName);
        }

    }

    //removes all users references from maps
    public void removeUser(String userName) throws Exception{
        users.remove(userName);
        userInput.remove(userName);
        userOutput.remove(userName);
        for(int i = 0; i<userNameList.size(); i++){
            if(userName.equals(userNameList.get(i))){
                userNameList.remove(i);
                userPublicKeyList.remove(i);
                removeKeys(userName);
                sendMessageAll(userName + " has Left");
            }
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

    //method for server to get messages from users
    public Object[] getMessage(String userName) throws Exception{
        return (Object[]) userInput.get(userName).readObject();
    }

    //method for server to send messages to users
    public void sendMessage( String userName, SealedObject message) throws Exception{
        userOutput.get(userName).writeObject(message);
    }

    //sends encrypted message to all users
    public void sendMessageAll(String message) throws Exception{

        for(int i = 0; i<userNameList.size(); i++){
            String user = userNameList.get(i);
            userOutput.get(user).writeObject(rsaUtil.encrypt(message,userPublicKeyList.get(user)));
        }
    }

    public void sendKeys(String userName, PublicKey key) throws Exception{

        String user;
        //sends new user key to all other users
        for(int i = 0; i<userNameList.size(); i++){
            Object[] items = new Object[3];
            user = userNameList.get(i);
            items[0] = "keyadd";
            items[1] = userName;
            items[2] = key;
            userOutput.get(user).writeObject(items);
        }
        //sends all user keys to new user
        for(int i = 0; i <userNameList.size(); i++){
            Object[] items = new Object[3];
            items[0] = "keyadd";
            items[1] = userNameList.get(i);
            items[2] = userPublicKeyList.get(userNameList.get(i));
            userOutput.get(userName).writeObject(items);

        }
    }

    public void sendServerKey(String userName) throws Exception{
        Object[] items = new Object[3];
        items[0] = "keyadd";
        items[1] = "Server";
        items[2] = rsaUtil.getPublicKey();
        userOutput.get(userName).writeObject(items);
    }

    public void removeKeys(String userName) throws Exception{
        String user;
        //sends new user key to all other users
        for(int i = 0; i<userNameList.size(); i++){
            Object[] items = new Object[3];
            user = userNameList.get(i);
            items[0] = "keyremove";
            items[1] = userName;
            userOutput.get(user).writeObject(items);

        }
    }

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
