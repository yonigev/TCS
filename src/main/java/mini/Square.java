package mini;

import net.iharder.dnd.FileDrop;
import org.apache.commons.net.ftp.FTPClient;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static mini.ClientMain.connectToServer;


/**
 * Connect and login to the FTP Server. (register to server if required.
 * @return the FTPClient
 */

public class Square extends ClientMain {
    public static final String WRITE_OPCODE = "write ";
    FileDrop fileDrop;
    FileDrop.Listener fileDropListener;
    private JPanel back;
    private JTable file_table;

    public Square() {

        GUI_connectToServer("127.0.0.1"); //TODO: change IP .
        fileDropListener = new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                for (File f : files) {
                    if (client.isConnected()) {
                        ClientHandler.handleWrite((WRITE_OPCODE + f.getPath()).split(" "));
                    }
                }
            }
        };
        fileDrop =new FileDrop(back,fileDropListener);
        updateFileTable(file_table);

    }

    public static void main(String[] args) {
        JFrame mainFrame = new JFrame("Square");      //create new JFrame

        mainFrame.setContentPane(new Square().back);    //set "back" as the content
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setSize(400,400);
        mainFrame.setVisible(true);

    }
    protected void GUI_connectToServer(String serverAddress) {
        try {
            client.connect(serverAddress, 44444);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {
            LoginRegister loginRegister = new LoginRegister();
            loginRegister.setSize(600,400);
            loginRegister.setVisible(true);
            if(!loginRegister.isSuccessful())
                System.exit(0);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    /**
     * Update the file JList according to the server's contents.
     * @param table
     */
    private void updateFileTable(JTable table){
        ArrayList<String> fileNames = ClientHandler.handleListCommand();

//        file_table.setListData(fileNames.toArray());
//        file_table.setVisibleRowCount(10);

    }



}
