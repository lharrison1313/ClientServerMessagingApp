import java.io.IOException;

public class ClientMessageReceiver implements Runnable {

    public Client client;

    public ClientMessageReceiver(Client client){
        this.client = client;
    }

    @Override
    public void run() {
        if(client != null){
            while(client.isOnline()){
                try{
                    System.out.println(client.getMessage());
                }
                catch(IOException e){
                    System.out.println(e);
                }
            }
        }
    }
}
