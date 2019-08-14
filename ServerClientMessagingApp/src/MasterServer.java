
import javax.crypto.SealedObject;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MasterServer {

    private Map<String,Integer> portList;
    private Map<String,Server> serverList;
    private ArrayList<String> servernameList;
    private User masterServerUser;
    private int portCount;
    RSA rsaUtil;
    DatabaseManager dbm;
    ServerSocket ss;

    public MasterServer() throws Exception{
        portList = new HashMap<>();
        serverList = new HashMap<>();
        servernameList = new ArrayList<>();
        portCount = 5051;
        rsaUtil = new RSA();
        masterServerUser = new User("MasterServer",rsaUtil.getPublicKey());
        ss = new ServerSocket(5050);

    }

    public void buildNewDatabase(String password, String sap)throws Exception{
        byte[] salt1 = RSA.generateSalt(32);
        byte[] salt2 = RSA.generateSalt(32);
        byte[] hash1 = RSA.hashPassword(password,salt1,100000);
        byte[] hash2 = RSA.hashPassword(sap, salt2, 100000);
        dbm = new DatabaseManager("MasterServer",hash1,salt1, hash2, salt2,true);
    }

    public void startMasterServer() throws Exception{
        Socket s;
        Thread ua;

        while(true){
            s = ss.accept();
            ua = new Thread(new ServerUserAcceptor(this,s));
            ua.start();
        }
    }

    public void acceptUser(Socket s) throws Exception{
        User clientUser;
        ObjectOutputStream output;
        ObjectInputStream input;
        boolean portSuccess = false;
        String servername;
        int port = 0;

        //1. getting input and output streams
        output = new ObjectOutputStream(s.getOutputStream());
        input = new ObjectInputStream(s.getInputStream());

        //2. sending server user object
        output.writeObject(masterServerUser);

        //3. getting client user object
        clientUser = (User) input.readObject();

        //4. getting port number for client
        while(!portSuccess){
            servername = rsaUtil.decrypt((SealedObject) input.readObject());
            if (isServerOnline(servername)) {
                portSuccess = true;
                port = getPort(servername);
                output.writeObject(rsaUtil.encrypt("true",clientUser.getPublicKey()));
            }else {
                port = 0;
                output.writeObject(rsaUtil.encrypt("false", clientUser.getPublicKey()));
            }
            output.writeObject(rsaUtil.encrypt(Integer.toString(port),clientUser.getPublicKey()));
        }

        s.close();


    }

    public boolean startServer(String serverName, String serverPassword) throws Exception{
        dbm = new DatabaseManager(serverName);
        byte[] salt = dbm.getUserSalt(serverName,serverName,true);
        boolean signInSuccess = false;
        if(salt != null){
            byte[] passwordHash = RSA.hashPassword(serverPassword,salt,100000);
            if(dbm.verifyPasswordHash(serverName,serverName,passwordHash,true)){
                servernameList.add(serverName);
                portList.put(serverName,portCount);
                Server s = new Server(portCount++,serverName);
                serverList.put(serverName,s);
                signInSuccess = true;
            }
        }

        return signInSuccess;

    }

    public void startServerCreator(String serverName, String serverPassword, String serverAccessPassword){
        Thread sc = new Thread(new ServerCreator(this,serverName,serverPassword,serverAccessPassword));
        sc.start();
    }

    public void startServerCreator(String serverName, String serverPassword){
        Thread sc = new Thread(new ServerCreator(this,serverName,serverPassword));
        sc.start();
    }

    public void createNewServer(String serverName, String serverPassword, String serverAccessPassword) throws Exception{
        byte[] salt1 = RSA.generateSalt(32);
        byte[] salt2 = RSA.generateSalt(32);
        byte[] password = RSA.hashPassword(serverPassword,salt1,100000);
        byte[] sap = RSA.hashPassword(serverAccessPassword,salt2,100000);

        servernameList.add(serverName);
        portList.put(serverName,portCount);
        Server s = new Server(portCount++,serverName,password,salt1,sap,salt2);
        serverList.put(serverName,s);

    }

    public int getPort(String servername){
        int port = 0;
        if(isServerOnline(servername)){
            port = portList.get(servername);
        }
        return port;
    }

    public boolean isServerOnline(String servername){
        return servernameList.contains(servername);
    }


    public static void main(String[] args){
        try {
            MasterServer m = new MasterServer();
            m.buildNewDatabase("rooster1","rooster2");
            m.startServerCreator("Server1","rooster1","rooster2");
            m.startMasterServer();
        }
        catch (Exception e){
            System.out.println(e);
        }
    }


}
