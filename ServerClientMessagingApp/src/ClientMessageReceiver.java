
/*
    purpose: the purpose of this class is to handle and process all incoming messages to the client.
    This class  is instantiated as a thread in the client class in order to continuously receive
    messages and commands from the server without interrupting processes occurring in the client;
 */

public class ClientMessageReceiver implements Runnable {

    public Client client;

    public ClientMessageReceiver(Client client){
        this.client = client;
    }

    @Override
    public void run() {
        if(client != null){
            //runs as long as the client is online
            while(client.isOnline()){
                try{
                    //getting an object from the clients input stream
                    Object item = client.getObject();

                    //if the object is a message it will be added to the message queue
                    if (item instanceof Message) {
                        client.decryptMessage((Message) item);
                    }
                    //if the object is a User it will be added to the clients userMap and userNameList
                    else if (item instanceof User) {
                        client.addUser((User) item);
                    }
                    //if the object is a Command it will processed by the client
                    else if (item instanceof Command) {
                        client.processCommand((Command) item);
                    }
                }
                catch(Exception e){
                    client.closeConnection();
                    System.out.println("there is a problem in cmr " + e);
                }
            }
        }
    }
}
