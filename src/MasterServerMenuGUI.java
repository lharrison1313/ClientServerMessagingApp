
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MasterServerMenuGUI {

    private JFrame frmMasterServerMenu;
    private JTextArea serverDialog;
    private JButton startServer;
    private JLabel serverPassword;
    private JComboBox<String> serverList;
    private JButton admin;
    private JPasswordField serverPasswordField;
    private MasterServer ms;
    private MasterServerMenuGUI msmg;

    public MasterServerMenuGUI(MasterServer ms){

        this.ms = ms;
        this.msmg = this;
        initialize();
        frmMasterServerMenu.setVisible(true);


    }

    private void initialize(){
        frmMasterServerMenu = new JFrame();
        frmMasterServerMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMasterServerMenu.setTitle("Master Server");
        frmMasterServerMenu.setSize(700,700);
        frmMasterServerMenu.setResizable(false);
        frmMasterServerMenu.setLocationRelativeTo(null);
        GridBagLayout gridBagLayout = new GridBagLayout();
        frmMasterServerMenu.getContentPane().setLayout(gridBagLayout);

        JLabel serverNames = new JLabel("Servers");
        GridBagConstraints serverNames_GBC = gbcBuilder(0,0,1,1,1,1);
        serverNames_GBC.fill = GridBagConstraints.HORIZONTAL;
        serverNames_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(serverNames,serverNames_GBC);

        //creating server list combo box
        serverList = new JComboBox<>();
        GridBagConstraints serverList_GBC = gbcBuilder(1,0,1,1,1,1);
        serverList_GBC.fill = GridBagConstraints.HORIZONTAL;
        serverList_GBC.insets = new Insets(10,10,10,10);

        //filling combobox with server names;
        for(String serverName: ms.getAllServerNames()){
            serverList.addItem(serverName);
        }

        frmMasterServerMenu.getContentPane().add(serverList,serverList_GBC);

        serverList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String server = (String) serverList.getSelectedItem();
                if(!ms.isServerOnline(server)){
                    startServer.setText("Start Server");
                    serverPassword.setVisible(true);
                    serverPasswordField.setVisible(true);
                    admin.setVisible(false);
                }
                else{
                    startServer.setText("Stop Server");
                    serverPassword.setVisible(false);
                    serverPasswordField.setVisible(false);
                    admin.setVisible(true);
                }

            }
        });



        JButton createNewServer = new JButton("Create new Server");
        GridBagConstraints createNewServer_GBC = gbcBuilder(2,0,1,1,1,1);
        serverList_GBC.fill = GridBagConstraints.HORIZONTAL;
        serverList_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(createNewServer,createNewServer_GBC);

        createNewServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ServerRegisterGUI srg = new ServerRegisterGUI(ms,msmg);
            }
        });

        serverPassword = new JLabel("Server Password");
        GridBagConstraints serverPassword_GBC = gbcBuilder(0,1,1,1,1,1);
        serverPassword_GBC.fill = GridBagConstraints.HORIZONTAL;
        serverPassword_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(serverPassword,serverPassword_GBC);


        serverPasswordField = new JPasswordField();
        GridBagConstraints serverPasswordField_GBC = gbcBuilder(1,1,2,1,1,1);
        serverPasswordField_GBC.fill = GridBagConstraints.HORIZONTAL;
        serverPasswordField_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(serverPasswordField,serverPasswordField_GBC);

        startServer = new JButton("Start Server");
        GridBagConstraints startServer_GBC = gbcBuilder(0,2,2,1,1,1);
        startServer_GBC.fill = GridBagConstraints.HORIZONTAL;
        startServer_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(startServer,startServer_GBC);

        startServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String server = (String) serverList.getSelectedItem();
                String password = new String(serverPasswordField.getPassword());
                if(!ms.isServerOnline(server)) {
                    try {
                        if (ms.startServer(server, password)) {
                            serverDialog.append(String.format("+ %s is online\n", server));
                            startServer.setText("Stop Server");
                            serverPasswordField.setVisible(false);
                            serverPassword.setVisible(false);
                            admin.setVisible(true);
                        } else {
                            serverDialog.append(String.format("- invalid credentials for %s\n", server));
                        }
                    } catch (Exception e1) {
                        System.out.println(e1);
                    }
                }
                else{
                    ms.stopServer(server);
                    startServer.setText("Start Server");
                    serverDialog.append(String.format("- %s is offline\n",server));
                    serverPasswordField.setVisible(true);
                    serverPassword.setVisible(true);
                    admin.setVisible(false);
                }
                serverPasswordField.setText("");

            }
        });

        admin = new JButton("Admin");
        GridBagConstraints admin_GBC = gbcBuilder(2,2,1,1,1,1);
        admin_GBC.fill = GridBagConstraints.HORIZONTAL;
        admin_GBC.insets = new Insets(10,10,10,10);
        admin.setVisible(false);
        admin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ServerAdminPanelGUI sapGUI = new ServerAdminPanelGUI(ms.getServer((String) serverList.getSelectedItem()));
            }
        });
        frmMasterServerMenu.getContentPane().add(admin,admin_GBC);

        JScrollPane serverDialogScrollPane = new JScrollPane();
        GridBagConstraints serverDialogScrollPane_GBC = gbcBuilder(0,3,3,1,1,1);
        serverDialogScrollPane_GBC.fill = GridBagConstraints.BOTH;
        serverDialogScrollPane_GBC.insets = new Insets(10,10,10,10);
        frmMasterServerMenu.getContentPane().add(serverDialogScrollPane,serverDialogScrollPane_GBC);

        serverDialog = new JTextArea();
        serverDialog.setEditable(false);
        serverDialogScrollPane.setViewportView(serverDialog);

    }

    private GridBagConstraints gbcBuilder(int gridx, int gridy, int gridWidth, int gridHeight, int weightx, int weighty){
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        return gbc;
    }

    public void refreshServerList(){
        for(int x = 0; x< serverList.getItemCount(); x++){
            serverList.removeItemAt(x);
        }
        for(String serverName: ms.getAllServerNames()){
            serverList.addItem(serverName);
        }
    }

    public static void main(String[] args){
        MasterServer ms = null;
        try{
            ms = new MasterServer(false);
        }
        catch (Exception e){
            System.out.println(e);
            try{
                ms = new MasterServer(true);
            }
            catch (Exception e1){
                System.out.println(e1);
            }
        }
        MasterServerMenuGUI msmg = new MasterServerMenuGUI(ms);
    }

}
