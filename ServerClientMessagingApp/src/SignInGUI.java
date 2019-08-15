import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SignInGUI {

    private JButton submit;
    private JLabel errorMessage;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField, sapTextField, confirmPasswordTextField;

    private JFrame frmSignIn;
    private Client client;
    boolean registerNewUser;


    public SignInGUI(Client client, boolean registerNewUser) throws Exception{
        this.registerNewUser = registerNewUser;
        this.client = client;
        initialize();
        frmSignIn.setVisible(true);
    }

    private void initialize(){

        frmSignIn = new JFrame();
        GridLayout gridLayout;
        frmSignIn.setLocationRelativeTo(null);

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

        JLabel sap = new JLabel("Server Access Password",SwingConstants.CENTER);
        frmSignIn.add(sap);

        sapTextField = new JPasswordField();
        frmSignIn.add(sapTextField);

        JLabel usernameLabel = new JLabel("Username",SwingConstants.CENTER);
        frmSignIn.add(usernameLabel);

        usernameTextField = new JTextField();
        frmSignIn.add(usernameTextField);

        JLabel passwordLabel = new JLabel("Password",SwingConstants.CENTER);
        frmSignIn.add(passwordLabel);

        passwordTextField = new JPasswordField();
        frmSignIn.add(passwordTextField);

        if(registerNewUser){
            JLabel confirmPasswordLabel = new JLabel("Confirm Password",SwingConstants.CENTER);
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
                        boolean loginSuccess = client.sendLogInInfo(registerNewUser,sap,username,password);

                        if (loginSuccess) {
                            frmSignIn.setVisible(false);
                            frmSignIn.dispose();
                            MessagingGUI messagingGUI = new MessagingGUI(client);
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
