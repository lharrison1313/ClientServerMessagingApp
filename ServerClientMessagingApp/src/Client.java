import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    //declaring socket, stream variables, and client info
    private Socket s;
    private DataInputStream input;
    private DataOutputStream output;
    private String userName;
    private boolean online;
    private Thread cms;
    private Thread cmr;

    public Client(String host, int port) throws IOException {
        online = true;

        //connects client to server and initializes data streams
        s = new Socket(host,port);
        input = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        output = new DataOutputStream(s.getOutputStream());

        //sends server username information and checks to see if username is taken
        boolean usernameSuccess;
        Scanner scan = new Scanner(System.in);
        do{
            System.out.println("please enter your username");
            this.userName = scan.nextLine();
            sendMessage(this.userName);
            usernameSuccess =  input.readBoolean();
            if(!usernameSuccess){
                System.out.println("username is taken or invalid please choose another");
            }
        }while(!usernameSuccess);

        //displays response from server
        System.out.println(getMessage());
        System.out.println("type $help for list of commands and features");

        //initializing message handler threads
        cms = new Thread(new ClientMessageSender(this));
        cmr = new Thread(new ClientMessageReceiver(this));

        //starting message handler threads
        cms.start();
        cmr.start();

    }

    //receives message from server
    public String getMessage() throws IOException{
        return input.readUTF();
    }

    //sends message to the server
    public void sendMessage(String message) throws IOException{
        output.writeUTF(message);
    }

    public void closeConnection() throws IOException{
        s.close();
        online = false;
    }

    public boolean isOnline(){
        return online;
    }


    public static void main(String[] args){
        try {
            Client c = new Client("localhost",5050);
        }
        catch(IOException e){
            System.out.println(e);
        }



    }

}
