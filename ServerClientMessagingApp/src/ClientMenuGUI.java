import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ClientMenuGUI {
    JFrame frmMenu;
    JTextField ipField;
    JTextField portField;
    JLabel ip;
    JLabel port;
    JButton register;
    JButton signin;


    public ClientMenuGUI(){
        initialize();
        frmMenu.setVisible(true);
    }

    public void initialize(){
        frmMenu = new JFrame();
        frmMenu.setTitle("Messaging app menu");
        frmMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMenu.setSize(500,150);
        frmMenu.setResizable(false);
        GridLayout gridLayout = new GridLayout(3,2);
        frmMenu.setLayout(gridLayout);

        ip = new JLabel("IP Address");
        frmMenu.add(ip);

        ipField = new JTextField();
        frmMenu.add(ipField);

        port = new JLabel("Port");
        frmMenu.add(port);

        portField = new JTextField();
        frmMenu.add(portField);

        signin = new JButton("Sign In");
        signin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                    Client c = new Client(ipField.getText(),Integer.parseInt(portField.getText()),false);
                    frmMenu.setVisible(false);
                    frmMenu.dispose();
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
                    Client c = new Client(ipField.getText(),Integer.parseInt(portField.getText()),true);
                    frmMenu.setVisible(false);
                    frmMenu.dispose();
                }
                catch(Exception e1){
                    System.out.println(e1);
                }
            }
        });
        frmMenu.add(register);



    }

    public static void main(String[] args){
        ClientMenuGUI cmg = new ClientMenuGUI();
    }

}
