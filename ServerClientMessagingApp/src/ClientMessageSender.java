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
        if(client != null){

            //runs while the client is connected to the server
            String userInput;
            while(client.isOnline()){
                try{
                    //gets input from user and checks for quit command
                    userInput = scan.nextLine();
                    if(userInput.substring(0,1).equals("@") && userInput.length() >1 && userInput.contains(" ")){
                        String[] messageList = userInput.split(" ", 2);

                        String receiver = messageList[0].substring(1);
                        userInput = messageList[1];

                        client.sendPrivateMessage(userInput,receiver);
                    }
                    else if(userInput.substring(0,1).equals("$") && userInput.length() >1 && !userInput.contains(" ")){
                        client.sendServerCommand(userInput.substring(1),"none");
                    }
                    else{
                        client.sendPublicMessage(userInput);
                    }

                }
                catch (Exception e){
                    System.out.println("There is a problem in cms " + e);
                }

            }
        }


    }
}
