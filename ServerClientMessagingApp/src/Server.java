import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Server  {

    //hash maps which hold user sockets and stream information
    private Map<String, Socket> users;
    private Map<String, DataInputStream> userInput;
    private Map<String, DataOutputStream> userOutput;
    private ArrayList<String> userNameList;



    public Server(int port) throws IOException{

        //initializing user hash maps
        users = new HashMap<String,Socket>();
        userInput = new HashMap<String,DataInputStream>();
        userOutput = new HashMap<String,DataOutputStream>();
        userNameList = new ArrayList<String>();


        //declaring socket and stream variables
        ServerSocket ss;
        Socket s;
        DataInputStream input;
        DataOutputStream output;
        String userName;
        Thread messageHandler;
        ss = new ServerSocket(port);

        while(true){
            s = ss.accept();

            //creating input and output data streams
            input = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            output = new DataOutputStream(s.getOutputStream());

            //getting username from client and checking if it already exists
            boolean usernameSuccess;
            do{
                userName = input.readUTF();
                if(userName.isEmpty() || userName.equals(" ") || userName.startsWith(" ")){
                    usernameSuccess = false;
                    output.writeBoolean(false);
                }
                else if(isUserOnline(userName)){
                    output.writeBoolean(false);
                    usernameSuccess = false;
                }
                else{
                    usernameSuccess = true;
                }
            }while(!usernameSuccess);
            output.writeBoolean(true);
            userNameList.add(userName);

            //placing user sockets and datastreams in hash maps
            users.put(userName, s);
            userInput.put(userName, input);
            userOutput.put(userName, output);

            //creating message handler thread for user
            messageHandler = new Thread(new ServerMessageHandler(this,userName));
            messageHandler.start();

            //displaying newly added user
            System.out.println("user " + userName + " has been added to the server");
            sendMessage("welcome to the server " + userName, userName);
            sendMessageAll(userName + " has joined the server");

        }

    }

    //removes all users references from maps
    public void removeUser(String userName){
        users.remove(userName);
        userInput.remove(userName);
        userOutput.remove(userName);
        for(int i = 0; i<userNameList.size(); i++){
            if(userName.equals(userNameList.get(i))){
                userNameList.remove(i);
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
    public String getMessage(String userName) throws IOException{
        return userInput.get(userName).readUTF();
    }

    //sends a single message to all users on the server
    public void sendMessageAll(String message) throws IOException{
        for(String users: userNameList){
            sendMessage(message,users);
        }
    }

    //method for server to send messages to users
    public void sendMessage(String message, String userName) throws IOException{
        userOutput.get(userName).writeUTF(message);
    }


    public ArrayList<String> getUserNameList(){
        return userNameList;
    }

    public static void main(String[] args){
        try{
            Server serve = new Server(5050);
        }
        catch (IOException e){
            System.out.println(e);
        }

    }


}
