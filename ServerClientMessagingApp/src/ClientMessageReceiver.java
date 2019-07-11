import javax.crypto.SealedObject;
import java.security.PublicKey;

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

                    Object item = client.getMessage();

                    if(item instanceof SealedObject){

                        client.printDecMessage((SealedObject) item);
                    }
                    else{
                        Object[] items = (Object[]) item;
                        String command = (String) items[0];
                        if(command.equals("keyadd")){
                            client.addKey((String) items[1],(PublicKey)items[2]);
                        }
                        else if(command.equals("keyremove")){
                            client.removeKey((String)items[1]);
                        }
                    }
                }
                catch(Exception e){

                    System.out.println(e);
                }
            }
        }
    }
}
