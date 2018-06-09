package mini;

import net.iharder.dnd.FileDrop;
import org.apache.commons.net.ftp.FTPClient;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
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
    static FTPClient client;
    static ClientHandler handler;

    public Square() {

        client = GUI_connectToServer("127.0.0.1"); //TODO: change IP .
        if (client != null) {
            handler = new ClientHandler(client, key1ForEncryption, key2ForAuthen);
        } else
            logger.info("Error:: client got null , can't handle");


        fileDropListener = new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                for (File f : files) {
                    if (client.isConnected()) {
                        handler.handleWrite((WRITE_OPCODE + f.getPath()).split(" "));
                    }
                }
            }
        };
        fileDrop =new FileDrop(back,fileDropListener);

    }

    public static void main(String[] args) {
        JFrame mainFrame = new JFrame("Square");      //create new JFrame
        mainFrame.setContentPane(new Square().back);    //set "back" as the content
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setSize(400,400);
        mainFrame.setVisible(true);



    }


    protected FTPClient GUI_connectToServer(String serverAddress) {
        client = new FTPClient();
        try {
            client.connect(serverAddress, 44444);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {
            LoginRegister loginRegister = new LoginRegister();
            loginRegister.setSize(600,400);
            loginRegister.setVisible(true);
        }

        return client;

    }

}
