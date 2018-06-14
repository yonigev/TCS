package mini;

import org.apache.commons.net.ftp.FTPClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginRegister extends JDialog {
    private static final String LOGIN_ERROR="Wrong username / password.";
    private static final String REGISTER_ERROR="Could not register";
    private static final String REGISTER_SUCCESS="Registration Successful";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea pleaseEnterUsernameAndTextArea;
    private JTextField username_field;
    private JTextField password_field;
    private JRadioButton loginSelection;
    private JRadioButton registerSelection;
    private ButtonGroup  group;
    private boolean login_successful = false;
    //private JPanel parent_panel;

    public LoginRegister() {
        setContentPane(contentPane);
        setModal(true);
        //this.parent_panel=panel;
        group=new ButtonGroup();
        group.add(loginSelection);
        group.add(registerSelection);
        pleaseEnterUsernameAndTextArea.setDisabledTextColor(Color.WHITE);
        password_field.

        getRootPane().setDefaultButton(buttonOK);


        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private void onOK() {
        // add your code here
        String username=username_field.getText();
        String password=password_field.getText();
        boolean success =   false;
        //Login / Register according to selection
        if(loginSelection.isSelected())
            success = Square.GUI_loginExistingAccount(Square.client,username,password, false);
        else if(registerSelection.isSelected())
            success = Square.GUI_registerNewAccount(username,password,Square.client);

        //if Registration or Login FAILED , show error.
        if(!success){
            if(loginSelection.isSelected())
                JOptionPane.showMessageDialog(null,LOGIN_ERROR);
            else if(registerSelection.isSelected())
                JOptionPane.showMessageDialog(null,REGISTER_ERROR);
        }
        //if Registration Successful, show message and Log in.
        if(success && registerSelection.isSelected()){
            loginSelection.setSelected(true);
            registerSelection.setSelected(false);
            JOptionPane.showMessageDialog(null,REGISTER_SUCCESS);
            success = Square.GUI_loginExistingAccount(Square.client,username,password, true);
        }

        if(success && loginSelection.isSelected()) {
            this.login_successful=true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }



    public boolean isSuccessful(){
        return this.login_successful;
    }
    public static void main(String[] args) {
        LoginRegister dialog = new LoginRegister();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}