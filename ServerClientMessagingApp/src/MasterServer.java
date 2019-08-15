
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
    private ArrayList<String> serverNameList;
    private User masterServerUser;
    private int portCount;
    RSA rsaUtil;
    DatabaseManager dbm;
    ServerSocket ss;
    Thread mss;

    public MasterServer(boolean newDatabase) throws Exception{
        portList = new HashMap<>();
        serverList = new HashMap<>();
        serverNameList = new ArrayList<>();
        portCount = 5051;
        rsaUtil = new RSA();
        masterServerUser = new User("MasterServer",rsaUtil.getPublicKey());
        ss = new ServerSocket(5050);
        dbm = new DatabaseManager(newDatabase);
        mss = new Thread(new MasterServerStarter(this));
        mss.start();


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

        //4. sending client serverLists
        output.writeObject(serverNameList);

        //5. getting port number for client
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

    private boolean serverSignIn(String serverName, String serverPassword) throws Exception{
        byte[] salt = dbm.getUserSalt(serverName,serverName,true);
        boolean signInSuccess = false;
        if(salt != null){
            byte[] passwordHash = RSA.hashPassword(serverPassword,salt,100000);
            if(dbm.verifyPasswordHash(serverName,serverName,passwordHash,true)){
                signInSuccess = true;
            }
        }

        return signInSuccess;

    }

    public boolean startServer(String serverName, String serverPassword) throws Exception{

        boolean signInSuccess = false;
        if(serverSignIn(serverName,serverPassword)){
            serverNameList.add(serverName);
            portList.put(serverName,portCount);
            Server s = new Server(portCount++,serverName,dbm);
            serverList.put(serverName,s);
            Thread sc = new Thread(new ServerCreator(s));
            sc.start();
            signInSuccess = true;
        }
        return signInSuccess;

    }

    public boolean stopServer(String serverName){
        boolean stopServerSuccess = false;
        if(isServerOnline(serverName)){
            if(serverList.get(serverName).closeConnection()){
                serverNameList.remove(serverName);
                serverList.remove(serverName);
                stopServerSuccess = true;
            }
        }
        return stopServerSuccess;

    }



    public boolean createNewServer(String serverName, String serverPassword, String serverAccessPassword) throws Exception{

        boolean newServerSuccess = false;

        if(!dbm.doesServerExist(serverName)) {
            byte[] salt1 = RSA.generateSalt(32);
            byte[] salt2 = RSA.generateSalt(32);
            byte[] password = RSA.hashPassword(serverPassword,salt1,100000);
            byte[] sap = RSA.hashPassword(serverAccessPassword,salt2,100000);
            portList.put(serverName, portCount);
            dbm.addNewServer(serverName, password, salt1, sap, salt2);
            newServerSuccess = true;
        }

        return newServerSuccess;


    }

    public int getPort(String serverName){
        int port = 0;
        if(isServerOnline(serverName)){
            port = portList.get(serverName);
        }
        return port;
    }

    public boolean isServerOnline(String servername){
        return serverNameList.contains(servername);
    }

    public ArrayList<String> getAllServerNames(){

        return dbm.getServerList();
    }

}
