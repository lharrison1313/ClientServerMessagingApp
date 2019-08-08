import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.*;

public class SignInGUI {

    JButton submit;
    JLabel errorMessage;
    JTextField usernameTextField,servernameTextField;
    JPasswordField passwordTextField, sapTextField;

    private JFrame frmSignIn;
    private Client client;
    private RSA rsaUtil;
    User serverUser;
    ObjectOutputStream output;
    ObjectInputStream input;
    boolean registerNewUser;

    public SignInGUI(Client client, User serverUser, ObjectOutputStream output, ObjectInputStream input, boolean registerNewUser) throws Exception{
        initialize();
        frmSignIn.setVisible(true);
        this.client = client;
        rsaUtil = new RSA();
        this.serverUser = serverUser;
        this.output = output;
        this.input = input;
        this.registerNewUser = registerNewUser;

    }

    private void initialize(){
        frmSignIn = new JFrame();
        frmSignIn.setTitle("Sign in");
        frmSignIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmSignIn.setSize(500,150);
        frmSignIn.setResizable(false);
        GridLayout gridLayout = new GridLayout(5,2);
        frmSignIn.setLayout(gridLayout);

        JLabel servername = new JLabel("Server Name");
        frmSignIn.add(servername);

        servernameTextField = new JTextField();
        frmSignIn.add(servernameTextField);

        JLabel sap = new JLabel("Server Access Password");
        frmSignIn.add(sap);

        sapTextField = new JPasswordField();
        frmSignIn.add(sapTextField);

        JLabel usernameLabel = new JLabel("Username");
        frmSignIn.add(usernameLabel);

        usernameTextField = new JTextField();
        frmSignIn.add(usernameTextField);

        JLabel passwordLabel = new JLabel("Password");
        frmSignIn.add(passwordLabel);

        passwordTextField = new JPasswordField();
        frmSignIn.add(passwordTextField);

        errorMessage = new JLabel();
        errorMessage.setForeground(Color.red);
        frmSignIn.add(errorMessage);

        submit = new JButton("Submit");
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                //getting username from text field
                String username = usernameTextField.getText();
                client.setUserName(username);

                //getting password from password field
                String password = new String(passwordTextField.getPassword());

                try {
                    output.writeBoolean(registerNewUser);
                    output.writeObject(rsaUtil.encrypt(username, serverUser.getPublicKey()));
                    output.writeObject(rsaUtil.encrypt(password, serverUser.getPublicKey()));

                    //getting login success response from server
                    String response = (String) input.readObject();


                    if (response.equals("false")) {
                        //resets text fields
                        errorMessage.setText("error");
                        servernameTextField.setText("");
                        sapTextField.setText("");
                        usernameTextField.setText("");
                        passwordTextField.setText("");
                    }
                    else{
                        //gets rid of jframe
                        frmSignIn.setVisible(false);
                        frmSignIn.dispose();
                        client.connectToServerP2();
                    }
                }
                catch (Exception e1){
                    System.out.println("error in sign in gui: " + e);
                }



            }
        });
        frmSignIn.add(submit);

    }



}
