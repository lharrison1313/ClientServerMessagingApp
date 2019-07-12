import javax.crypto.SealedObject;
import java.security.PublicKey;

public class ClientMessageReceiver implements Runnable {

    public Client client;
    public RSA rsaUtil;

    public ClientMessageReceiver(Client client){
        this.client = client;
        rsaUtil = client.getRsaUtil();
    }

    @Override
    public void run() {
        if(client != null){
            while(client.isOnline()){
                try{

                    Object item = client.getObject();

                    if(item instanceof Message){
                       client.decryptMessage((Message) item);
                    }
                    else if(item instanceof User){
                        client.addUser((User) item);
                    }
                    else if(item instanceof Command){
                        Command c = (Command) item;
                        String command = rsaUtil.decrypt(c.getCommand());
                        String option = rsaUtil.decrypt(c.getOption());
                        switch(command){
                            case "removeuser":
                                client.removeUser(option);
                                break;
                            default:
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
