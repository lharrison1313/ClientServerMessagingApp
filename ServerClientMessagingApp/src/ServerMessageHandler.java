import java.io.IOException;
import java.util.ArrayList;

public class ServerMessageHandler implements  Runnable{
    private Server server;
    private ArrayList<String> userNameList;
    private String senderName;
    private boolean online;

    public ServerMessageHandler(Server server, String userName){
        this.server = server;
        this.userNameList = server.getUserNameList();
        this.senderName = userName;
        this.online = true;


    }

    private String[] messageParser(String message,String regex,int limit){
        return message.split(regex,limit);
    }

    @Override
    public void run() {
        if(server != null){

            String message;
            String receiver;
            String[] messageArray;
            System.out.println("user " + senderName + " message handler is running");


            while(online){
                try{
                    message = server.getMessage(senderName);

                    if(message.equals("")){
                        //do nothing
                    }
                    //private messages
                    else if(message.substring(0,1).equals("@") && message.length() >1 && message.contains(" ")){

                        messageArray = messageParser(message," ",2);
                        receiver = messageArray[0].substring(1);
                        message = senderName + ": " + messageArray[1];

                        if(server.isUserOnline(receiver) == false){
                            server.sendMessage("Server: user doesent exist",senderName);
                        }
                        else{
                            server.sendMessage("pm"+ " " + message, receiver);
                            server.sendMessage("pm" + " " + message, senderName);
                        }

                    }
                    //server commands
                    else if(message.substring(0,1).equals("$")){
                        messageArray = messageParser(message," ",2);
                        switch(messageArray[0]){
                            case "$users":
                                server.sendMessage("all users:", senderName);
                                for(String users: userNameList){
                                    server.sendMessage(users,senderName);
                                }
                                break;
                            case "$help":
                                server.sendMessage("Use @username to private message \nList of commands \n$users: lists users \n$help: lists help screen \n$quit leaves the server", senderName);
                                break;
                            case "$quit":
                                server.sendMessageAll(senderName + " has left the server");
                                server.removeUser(senderName);
                                online = false;
                                break;

                            default:
                                server.sendMessageAll(senderName + ": " + message);
                        }

                    }
                    //server chat
                    else{
                        server.sendMessageAll(senderName + ": " + message);
                    }


                }
               catch(IOException e){
                    System.out.println(e);
                }

            }
            System.out.println("user" + senderName + "has left the server");
        }
    }
}
