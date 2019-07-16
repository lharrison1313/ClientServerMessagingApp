import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;

public class MessagingGUI {

        private JFrame frmMessagingApp;
        private JTextField textField;
        private JTextArea textArea;
        private JButton btnNewButton;

        public MessagingGUI() {
            initialize();
            frmMessagingApp.getRootPane().setDefaultButton(btnNewButton);
            frmMessagingApp.setVisible(true);
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

            GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
            gbc_btnNewButton.insets = new Insets(0, 10, 10, 10);
            gbc_btnNewButton.gridx = 1;
            gbc_btnNewButton.gridy = 1;
            frmMessagingApp.getContentPane().add(btnNewButton, gbc_btnNewButton);
        }

        public JButton getButton() {
            return btnNewButton;
        }

        public JTextField getTextField(){
            return textField;
        }

        public void appendTextArea(String text){
            textArea.append(text + "\n");
        }

        public Frame getFrame(){
            return frmMessagingApp;
        }



}
