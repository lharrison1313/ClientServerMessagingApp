import java.net.Socket;

public class ServerUserAcceptor implements Runnable {
    Server server;
    MasterServer masterServer;
    Socket socket;


    ServerUserAcceptor(Server serve, Socket sock){
        server = serve;
        socket = sock;
    }
    ServerUserAcceptor(MasterServer serve, Socket sock){
        masterServer = serve;
        socket = sock;
    }

    @Override
    public void run(){
        try {
            if(server != null){
                server.acceptUser(socket);
            }
            else{
                masterServer.acceptUser(socket);
            }
        }
        catch (Exception e){
            System.out.println(e);
        }
    }
}
