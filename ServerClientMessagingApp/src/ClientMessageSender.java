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
                    if(message.substring(0,1).equals("@") && message.length() >1 && message.contains(" ")){
                        String[] messageList = message.split(" ", 2);

                        String receiver = messageList[0].substring(1);
                        message = messageList[1];

                        client.sendPrivateMessage(message,receiver);
                    }
                    else if(message.substring(0,1).equals("$") && message.length() >1 && !message.contains(" ")){
                        client.sendServerCommand(message.substring(1),"none");
                    }
                    else{
                        client.sendPublicMessage(message);
                    }

                }
                catch (Exception e){
                    System.out.println(e);
                }

            }
        }


    }
}
