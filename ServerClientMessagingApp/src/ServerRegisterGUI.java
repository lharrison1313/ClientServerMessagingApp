import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

public class ServerRegisterGUI {

    private JFrame frmServerRegister;
    private MasterServer ms;
    private MasterServerMenuGUI msmg;

    public ServerRegisterGUI(MasterServer ms, MasterServerMenuGUI msmg){
        this.ms = ms;
        this.msmg = msmg;
        initialize();
        frmServerRegister.setVisible(true);
    }

    public void initialize(){
        frmServerRegister = new JFrame();
        frmServerRegister.setSize(600,400);
        frmServerRegister.setLocationRelativeTo(null);
        GridLayout layout = new GridLayout(6,2);
        frmServerRegister.setLayout(layout);

        JLabel serverName = new JLabel("Server Name: ");
        frmServerRegister.add(serverName);

        JTextField serverNameField = new JTextField();
        frmServerRegister.add(serverNameField);

        JLabel serverPassword = new JLabel("Server Password: ");
        frmServerRegister.add(serverPassword);

        JPasswordField serverPasswordField = new JPasswordField();
        frmServerRegister.add(serverPasswordField);

        JLabel confirmServerPassword = new JLabel("Confirm Server Password: ");
        frmServerRegister.add(confirmServerPassword);

        JPasswordField confirmServerPasswordField = new JPasswordField();
        frmServerRegister.add(confirmServerPasswordField);

        JLabel sap = new JLabel("Server Access Password: ");
        frmServerRegister.add(sap);

        JPasswordField sapField = new JPasswordField();
        frmServerRegister.add(sapField);

        JLabel confirmSap = new JLabel("Confirm Server Access Password");
        frmServerRegister.add(confirmSap);

        JPasswordField confirmSapField = new JPasswordField();
        frmServerRegister.add(confirmSapField);

        JLabel errorMessage = new JLabel("");
        frmServerRegister.add(errorMessage);

        JButton registerServer = new JButton("Register");
        frmServerRegister.add(registerServer);
        registerServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverName = serverNameField.getText();
                String password = new String(serverPasswordField.getPassword());
                String passwordConfirm = new String(confirmServerPasswordField.getPassword());
                String sap = new String(sapField.getPassword());
                String sapConfirm = new String(confirmSapField.getPassword());

                if(password.equals(passwordConfirm) && sap.equals(sapConfirm) && !ms.isServerOnline(serverName)){
                    if(!sap.equals("") || !serverName.equals("") || !password.equals("") ) {
                        try {
                            ms.createNewServer(serverName, password, sap);
                        } catch (Exception e1) {
                            System.out.println(e);
                        }
                        msmg.refreshServerList();
                        frmServerRegister.dispatchEvent(new WindowEvent(frmServerRegister, WindowEvent.WINDOW_CLOSING));
                    }
                    else{
                        errorMessage.setText("one or more items blank");
                    }
                }
                else{
                    errorMessage.setText("one or more password items do not match");
                }

            }
        });




    }

}
