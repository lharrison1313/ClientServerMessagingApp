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
                    Object item = client.getObject();

                    if (item instanceof Message) {
                        client.decryptMessage((Message) item);
                    } else if (item instanceof User) {
                        client.addUser((User) item);
                    } else if (item instanceof Command) {
                        client.processCommand((Command) item);
                    }
                }
                catch(Exception e){

                    System.out.println("there is a problem in cmr " + e);
                }
            }
        }
    }
}
