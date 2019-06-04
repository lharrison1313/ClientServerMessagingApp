import java.io.IOException;
import java.util.Scanner;

public class ClientMessageSender implements Runnable{

    private Client client;
    private Scanner scan;

    public ClientMessageSender(Client client){
        this.client = client;
        scan = new Scanner(System.in);
    }

    @Override
    public void run() {
        System.out.println("type to send message or use @user to pm someone");
        if(client != null){

            //runs while the client is connected to the server
            String message;
            while(client.isOnline()){
                try{
                    //gets input from user and checks for quit command
                    message = scan.nextLine();
                    if(message.contains("$quit")){
                        client.sendMessage("$quit");
                        client.closeConnection();
                    }
                    else{
                        client.sendMessage(message);
                    }

                }
                catch (IOException e){
                    System.out.println(e);
                }

            }
        }


    }
}
