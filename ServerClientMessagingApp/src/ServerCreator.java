public class ServerCreator implements Runnable {

    private MasterServer ms;
    private String serverName, serverPassword, serverAccessPassword;

    public ServerCreator(MasterServer ms, String serverName, String serverPassword){
        this.ms = ms;
        this.serverName = serverName;
        this.serverPassword = serverPassword;
        this.serverAccessPassword = null;
    }

    public ServerCreator(MasterServer ms, String serverName, String serverPassword, String serverAccessPassowrd){
        this.ms = ms;
        this.serverName = serverName;
        this.serverPassword = serverPassword;
        this.serverAccessPassword = serverAccessPassowrd;
    }

    @Override
    public void run() {
        try {
            if (serverAccessPassword == null) {
                ms.startServer(serverName, serverPassword);
            }
            else {
                ms.createNewServer(serverName, serverPassword, serverAccessPassword);
            }
        }
        catch (Exception e){
            System.out.println(e);
        }

    }
}
