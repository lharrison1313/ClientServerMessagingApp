import java.net.Socket;

public class ServerUserAcceptor implements Runnable {
    Server server;
    Socket socket;
    ServerUserAcceptor(Server serve, Socket sock){
        server = serve;
        socket = sock;
    }

    @Override
    public void run(){
        try {
            server.acceptUser(socket);
        }
        catch (Exception e){
            System.out.println("problem in server user acceptor");
        }
    }
}
