import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.crypto.SealedObject;
import javax.swing.*;

public class ClientMenuGUI {
    private JFrame frmMenu;
    private JTextField serverNameField;
    private JLabel serverName;
    private JButton register;
    private JButton signin;
    private User masterServerUser;
    public User clientUser;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private RSA rsaUtil;
    private int masterServerPort;
    private String masterServerHost;



    public ClientMenuGUI(String masterServerHost, int masterServerPort) throws Exception{
        this.masterServerHost = masterServerHost;
        this.masterServerPort = masterServerPort;
        rsaUtil = new RSA();
        clientUser = new User("tempuser",rsaUtil.getPublicKey());
        connectToServer();
        initialize();
        frmMenu.setVisible(true);



    }

    public void initialize() throws Exception{
        frmMenu = new JFrame();
        frmMenu.setTitle("Messaging app menu");
        frmMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMenu.setSize(500,150);
        frmMenu.setLocationRelativeTo(null);
        frmMenu.setResizable(false);
        GridLayout gridLayout = new GridLayout(2,2);
        frmMenu.setLayout(gridLayout);

        serverName = new JLabel("Server Name", SwingConstants.CENTER);
        frmMenu.add(serverName);

        serverNameField = new JTextField();
        frmMenu.add(serverNameField);

        signin = new JButton("Sign In");
        signin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                    String portSuccess;
                    String serverName = serverNameField.getText();
                    if(!serverName.equals("")) {
                        output.writeObject(rsaUtil.encrypt(serverNameField.getText(), masterServerUser.getPublicKey()));
                        portSuccess = rsaUtil.decrypt((SealedObject) input.readObject());
                        String port = rsaUtil.decrypt((SealedObject) input.readObject());
                        if (portSuccess.equals("true")) {
                            Client c = new Client(masterServerHost, Integer.parseInt(port));
                            SignInGUI signInGUI = new SignInGUI(c,false);
                            frmMenu.setVisible(false);
                            frmMenu.dispose();
                        }
                    }
                }
                catch(Exception e1){
                    System.out.println(e1);
                }
            }
        });
        frmMenu.add(signin);

        register = new JButton("Register");
        register.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    String serverName = serverNameField.getText();
                    String portSuccess;
                    if(!serverName.equals("")) {
                        output.writeObject(rsaUtil.encrypt(serverName, masterServerUser.getPublicKey()));
                        portSuccess = rsaUtil.decrypt((SealedObject) input.readObject());
                        String port = rsaUtil.decrypt((SealedObject) input.readObject());

                        if (portSuccess.equals("true")) {
                            Client c = new Client(masterServerHost, Integer.parseInt(port));
                            SignInGUI signInGUI = new SignInGUI(c,true);
                            frmMenu.setVisible(false);
                            frmMenu.dispose();
                        }
                    }

                }
                catch(Exception e1){
                    System.out.println(e1);
                }
            }
        });
        frmMenu.add(register);

    }

    public void connectToServer()throws Exception{

        Socket s = new Socket(masterServerHost,masterServerPort);
        System.out.println("1");
        //1.getting input and output streams
        input = new ObjectInputStream(s.getInputStream());
        output = new ObjectOutputStream(s.getOutputStream());
        System.out.println("2");
        //2. getting server user info
        masterServerUser = (User) input.readObject();

        //3. sending server client user info
        output.writeObject(clientUser);
        System.out.println("3");


    }

    public static void main(String[] args){
        try{
            ClientMenuGUI cmg = new ClientMenuGUI("localhost",5050);
        }
        catch (Exception e){
            System.out.println(e);
        }

    }

}
