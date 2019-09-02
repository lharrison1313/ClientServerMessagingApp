
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ServerAdminPanelGUI {

    JFrame serverAdminPanelFrame;
    JComboBox<String> userList;
    JComboBox<String> privilegeList;
    Server s;

    ServerAdminPanelGUI(Server s){
        this.s = s;
        initialize();
    }

    public void initialize(){

        serverAdminPanelFrame = new JFrame();
        GridLayout gridLayout = new GridLayout();
        gridLayout.setColumns(3);
        gridLayout.setRows(2);
        serverAdminPanelFrame.setLayout(gridLayout);
        serverAdminPanelFrame.setVisible(true);
        serverAdminPanelFrame.setSize(500,200);
        serverAdminPanelFrame.setResizable(false);
        serverAdminPanelFrame.setTitle(s.getServerName() + " Admin Panel");

        JLabel userPrivilege = new JLabel("User Privilege");

        JLabel users = new JLabel("Users");
        serverAdminPanelFrame.add(users);

        userList = new JComboBox<>();
        refreshUserList();
        userList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int privilege = 0;
                String name = (String)userList.getSelectedItem();

                try {
                    privilege = s.getUserPrivilege((String) userList.getSelectedItem());
                }
                catch (Exception e1){
                    System.out.println(e1);
                }

                userPrivilege.setText(name + "'s privilege: " + privilege);
            }
        });
        serverAdminPanelFrame.add(userList);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshUserList();
            }
        });
        serverAdminPanelFrame.add(refresh);

        serverAdminPanelFrame.add(userPrivilege);

        privilegeList = new JComboBox<>();
        privilegeList.addItem("Muted: 0");
        privilegeList.addItem("User: 1");
        privilegeList.addItem("Admin: 2");
        privilegeList.addItem("Owner: 3");
        serverAdminPanelFrame.add(privilegeList);

        JButton setPrivilege = new JButton("Set Privilege");
        setPrivilege.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = (String)userList.getSelectedItem();
                int privilege = 0;

                try {
                    s.setUserPrivilege(name, privilegeList.getSelectedIndex());
                    privilege = s.getUserPrivilege(name);
                    userPrivilege.setText(name + "'s privilege: " + privilege);
                }
                catch (Exception e1){
                    System.out.println(e1);
                }

            }
        });
        serverAdminPanelFrame.add(setPrivilege);


    }

    public void refreshUserList(){
        ArrayList<String> userArrayList = s.getAllUsers();
        userList.removeAllItems();
        for(String user: userArrayList){
            userList.addItem(user);
        }
    }


}
