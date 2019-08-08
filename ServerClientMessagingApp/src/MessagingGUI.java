import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class MessagingGUI {

        private JFrame frmMessagingApp;
        private JTextField textField;
        private JTextArea textArea;
        private JButton btnNewButton;
        private Client client;

        public MessagingGUI(Client client) {
            initialize();
            frmMessagingApp.getRootPane().setDefaultButton(btnNewButton);
            frmMessagingApp.setVisible(true);
            this.client = client;
        }

        private void initialize() {
            frmMessagingApp = new JFrame();
            frmMessagingApp.setTitle("Messaging App");
            frmMessagingApp.setBounds(100, 100, 450, 300);
            frmMessagingApp.setSize(750,500);
            frmMessagingApp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GridBagLayout gridBagLayout = new GridBagLayout();
            gridBagLayout.columnWidths = new int[]{0, 0, 0};
            gridBagLayout.rowHeights = new int[]{0, 0, 0};
            gridBagLayout.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
            gridBagLayout.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
            frmMessagingApp.getContentPane().setLayout(gridBagLayout);

            JScrollPane scrollPane = new JScrollPane();
            GridBagConstraints gbc_scrollPane = new GridBagConstraints();
            gbc_scrollPane.fill = GridBagConstraints.BOTH;
            gbc_scrollPane.gridwidth = 2;
            gbc_scrollPane.insets = new Insets(10, 10, 10, 10);
            gbc_scrollPane.gridx = 0;
            gbc_scrollPane.gridy = 0;
            frmMessagingApp.getContentPane().add(scrollPane, gbc_scrollPane);

            textArea = new JTextArea();
            textArea.setEditable(false);
            scrollPane.setViewportView(textArea);

            textField = new JTextField();
            GridBagConstraints gbc_textField = new GridBagConstraints();
            gbc_textField.insets = new Insets(0, 10, 10, 5);
            gbc_textField.fill = GridBagConstraints.HORIZONTAL;
            gbc_textField.gridx = 0;
            gbc_textField.gridy = 1;
            frmMessagingApp.getContentPane().add(textField, gbc_textField);
            textField.setColumns(10);

            btnNewButton = new JButton("Send");

            btnNewButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    boolean close;
                    String userInput = textField.getText();
                    textField.setText("");
                    close = client.sendMessage(userInput);
                    if(close == true){
                        frmMessagingApp.dispatchEvent(new WindowEvent(frmMessagingApp,WindowEvent.WINDOW_CLOSING));
                    }
                }
            });

            GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
            gbc_btnNewButton.insets = new Insets(0, 10, 10, 10);
            gbc_btnNewButton.gridx = 1;
            gbc_btnNewButton.gridy = 1;
            frmMessagingApp.getContentPane().add(btnNewButton, gbc_btnNewButton);

            frmMessagingApp.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    try{
                        client.sendServerCommand("quit","none");
                    }
                    catch (Exception e1){
                        System.out.println("error sending server quit command");
                    }
                    e.getWindow().dispose();
                }
            });
        }

        public void appendTextArea(String text){
            textArea.append(text + "\n");
        }

}
