import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.*;

public class SignInGUI {

    JButton submit;
    JLabel errorMessage;
    JTextField usernameTextField;
    JPasswordField passwordTextField, sapTextField, confirmPasswordTextField;

    private JFrame frmSignIn;
    private Client client;
    private RSA rsaUtil;
    User serverUser;
    ObjectOutputStream output;
    ObjectInputStream input;
    boolean registerNewUser;

    public SignInGUI(Client client, User serverUser, ObjectOutputStream output, ObjectInputStream input, boolean registerNewUser) throws Exception{
        this.registerNewUser = registerNewUser;
        this.client = client;
        rsaUtil = new RSA();
        this.serverUser = serverUser;
        this.output = output;
        this.input = input;
        initialize();
        frmSignIn.setVisible(true);
    }

    private void initialize(){

        frmSignIn = new JFrame();
        GridLayout gridLayout;

        if(registerNewUser) {
            frmSignIn.setTitle("Register");
            gridLayout = new GridLayout(5,2);
        }
        else{
            frmSignIn.setTitle("Sign in");
            gridLayout = new GridLayout(4,2);
        }

        frmSignIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmSignIn.setSize(600,150);
        frmSignIn.setResizable(false);

        frmSignIn.setLayout(gridLayout);

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

        if(registerNewUser){
            JLabel confirmPasswordLabel = new JLabel("Confirm Password");
            frmSignIn.add(confirmPasswordLabel);
            confirmPasswordTextField = new JPasswordField();
            frmSignIn.add(confirmPasswordTextField);

        }

        errorMessage = new JLabel();
        errorMessage.setForeground(Color.red);
        frmSignIn.add(errorMessage);

        submit = new JButton("Submit");
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String username, password, sap;
                String passwordConfirm = "";

                //getting username from text field
                username = usernameTextField.getText();
                client.setUserName(username);

                //getting passwords from password field
                password = new String(passwordTextField.getPassword());
                if(registerNewUser)
                    passwordConfirm = new String(confirmPasswordTextField.getPassword());
                sap = new String(sapTextField.getPassword());

                if(!registerNewUser || password.equals(passwordConfirm) ) {
                    try {
                        //sending login info to server
                        output.writeBoolean(registerNewUser);
                        output.writeObject(rsaUtil.encrypt(sap, serverUser.getPublicKey()));
                        output.writeObject(rsaUtil.encrypt(username, serverUser.getPublicKey()));
                        output.writeObject(rsaUtil.encrypt(password, serverUser.getPublicKey()));

                        //getting login success response from server
                        String response = (String) input.readObject();

                        if (response.equals("true")) {
                            frmSignIn.setVisible(false);
                            frmSignIn.dispose();
                            client.connectToServerP2();
                        }
                        else{
                            errorMessage.setText("incorrect server access password or login info");
                        }

                    } catch (Exception e1) {
                        System.out.println("error in sign in gui: " + e);
                    }
                }
                else{
                    errorMessage.setText("passwords don't match");
                }

                sapTextField.setText("");
                usernameTextField.setText("");
                passwordTextField.setText("");

                if(registerNewUser)
                    confirmPasswordTextField.setText("");

            }
        });
        frmSignIn.add(submit);

    }



}
