public class ServerCreator implements Runnable {

    Server s;

    public ServerCreator(Server s){
        this.s = s;
    }

    @Override
    public void run() {
        try {
            s.startServer();
        }
        catch (Exception e){
            System.out.println(e);
        }

    }
}
