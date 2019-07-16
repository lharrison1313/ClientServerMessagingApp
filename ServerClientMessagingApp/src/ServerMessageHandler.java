import java.util.ArrayList;

public class ServerMessageHandler implements  Runnable{
    private Server server;
    private ArrayList<String> userNameList;
    private String senderName;
    private RSA rsaUtil;

    public ServerMessageHandler(Server server, String userName, RSA rsaUtil){
        this.server = server;
        this.userNameList = server.getUserNameList();
        this.senderName = userName;
        this.rsaUtil = rsaUtil;


    }

    @Override
    public void run() {
        if(server != null){

            Object item;
            System.out.println("user " + senderName + " message handler is running");

            while(server.isUserOnline(senderName)){
                try{

                     item = server.getObject(senderName);
                     //if the object is a message relay the message to the recipient
                     if(item instanceof Message){
                         server.relayMessage((Message) item);
                     }
                     //if the object is a command execute the command
                     else if(item instanceof  Command){
                         server.commandProcessor((Command) item, senderName);
                     }

                }
               catch(Exception e){
                    System.out.println(e);
                }

            }
            System.out.println("user " + senderName + " has left the server");
        }
    }
}
