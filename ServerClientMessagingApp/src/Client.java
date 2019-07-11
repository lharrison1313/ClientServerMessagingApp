import javax.crypto.SealedObject;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.security.PublicKey;
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
    private Map<String, PublicKey> userPublicKeyList;
    private ArrayList<String> userNameList;
    private RSA rsaUtil;

    public Client(String host, int port) throws Exception {

        online = true;

        //connects client to server and initializes data streams
        s = new Socket(host,port);

        input = new ObjectInputStream(s.getInputStream());
        output = new ObjectOutputStream(s.getOutputStream());

        //initializing public key hash map and rsa utility
        userPublicKeyList = new HashMap<String,PublicKey>();
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
        userPublicKeyList.put(this.userName,rsaUtil.getPublicKey());
        userNameList.add(userName);
        output.writeObject(rsaUtil.getPublicKey());



        //displays response from server
        //getMessage();
        System.out.println("type $help for list of commands and features");

        //initializing message handler threads
        cms = new Thread(new ClientMessageSender(this));
        cmr = new Thread(new ClientMessageReceiver(this));

        //starting message handler threads
        cms.start();
        cmr.start();



    }


    public void sendPrivateMessage(String message, String userName) throws Exception{
        SealedObject[] items = new SealedObject[2];
        if(!userName.equals("Server") && userNameList.contains(userName)){
            items[0] = rsaUtil.encrypt(userName,userPublicKeyList.get("Server"));
            items[1] = rsaUtil.encrypt("~" + this.userName + ": " +message,userPublicKeyList.get(userName));
            System.out.println("~" + this.userName + ": " +message);
            output.writeObject(items);
        }
        else{
            System.out.println("user "+ userName + " does not exist");
        }

    }

    public void sendPublicMessage(String message) throws Exception{
        for(int x = 0; x < userNameList.size(); x++){
            SealedObject[] items = new SealedObject[2];
            String userName = userNameList.get(x);
            if(!userName.equals("Server")){
                items[0] = rsaUtil.encrypt(userName,userPublicKeyList.get("Server"));
                items[1] = rsaUtil.encrypt(this.userName+ ": " + message,userPublicKeyList.get(userName));
                output.writeObject(items);
            }
        }
    }

    public void sendServerCommand(String command) throws Exception{
        switch (command){
            case "quit":
                SealedObject[] items = new SealedObject[2];
                items[0] = rsaUtil.encrypt("Server",userPublicKeyList.get("Server"));
                items[1] = rsaUtil.encrypt(command,userPublicKeyList.get("Server"));
                output.writeObject(items);
                closeConnection();
                break;
            case "users":
                for(int x = 0; x < userNameList.size(); x++){
                    if(!userNameList.get(x).equals("Server")){
                        System.out.println(userNameList.get(x));
                    }
                }
                break;
            case "help":
                System.out.println("Command List:\n$quit: closes server connection\n$users: displays all online users\n@[username]: sends private message to a user");
                break;
            default:
                System.out.println("Command not recognized");
                break;

        }

        if(command.equals("quit")){

        }

    }

    public Object getMessage() throws Exception{
        Object item = input.readObject();
        return item;
    }

    public void printDecMessage(SealedObject message) throws Exception{
        System.out.println(rsaUtil.decrypt(message));
    }

    public void closeConnection() throws IOException{
        s.close();
        online = false;
    }

    public boolean isOnline(){
        return online;
    }

    public void addKey(String name, PublicKey publicKey){
        if(!userNameList.contains(name)) {
            userNameList.add(name);
            userPublicKeyList.put(name, publicKey);
        }
    }

    public void removeKey(String name){
        userNameList.remove(name);
        userPublicKeyList.remove(name);
        System.out.println("key deleted");
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
