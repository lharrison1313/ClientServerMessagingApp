import javax.crypto.SealedObject;
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

    private String[] messageParser(String message,String regex,int limit){
        return message.split(regex,limit);
    }

    @Override
    public void run() {
        if(server != null){

            Object[] items;
            String command;
            String option;
            String receiver;
            SealedObject message;
            System.out.println("user " + senderName + " message handler is running");

            while(server.isUserOnline(senderName)){
                try{

                    items = server.getMessage(senderName);
                    receiver = rsaUtil.decrypt((SealedObject) items[0]);
                    if(receiver.equals("Server")){
                        command = rsaUtil.decrypt((SealedObject)items[1]);
                        switch (command){
                            case("quit"):
                                server.removeUser(senderName);
                                break;
                        }
                    }
                    else{
                        message = (SealedObject)items[1];
                        server.sendMessage(receiver,message);
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
