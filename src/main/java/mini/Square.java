package mini;

import net.iharder.dnd.FileDrop;
import org.apache.commons.net.ftp.FTPClient;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.sun.deploy.uitoolkit.ToolkitStore.dispose;


/**
 * Connect and login to the FTP Server. (register to server if required.
 * @return the FTPClient
 */

public class Square extends ClientMain  {
    public static final String WRITE_OPCODE = "write ";
    private static final String[] TABLE_HEADER={"Name ","Size ","Last Modified "};
    FileDrop fileDrop;
    FileDrop.Listener fileDropListener;
    private JPanel back;
    private JTable file_table;
    private JScrollPane scrollPane;
    private JButton buttonDelete;
    private JButton buttonExit;
    private static JFrame mainFrame;
    private DeleteFileActionListener deleteFileActionListener;

    public Square() {
        GUI_connectToServer("127.0.0.1"); //TODO: change IP .
        deleteFileActionListener=new DeleteFileActionListener();
        fileDropListener = new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                for (File f : files) {
                    if (client.isConnected()) {
                        ClientHandler.handleWrite((WRITE_OPCODE + f.getPath()).split(" "));
                        updateFileTable();
                    }
                }
            }
        };
        fileDrop =new FileDrop(back,fileDropListener);
        updateFileTable();
        buttonExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onExit();
            }
        });
        buttonDelete.addActionListener(deleteFileActionListener);

    }

    public static void main(String[] args) {

        mainFrame = new JFrame("Square");      //create new JFrame

        mainFrame.setContentPane(new Square().back);    //set "back" as the content
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setSize(400,400);
        mainFrame.setVisible(true);

    }
    protected void GUI_connectToServer(String serverAddress) {
        try {
            client.connect(serverAddress, 44444);
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {

            LoginRegister loginRegister = new LoginRegister();
            loginRegister.setSize(500,300);
            loginRegister.setResizable(false);
            loginRegister.setVisible(true);
            if(!loginRegister.isSuccessful())
                System.exit(0);
        }
    }

    private void createUIComponents() {
        //create the JScrollPane
        scrollPane=new JScrollPane(file_table);
    }
    /**
     * Update the file JList according to the server's contents.
     */
    private void updateFileTable(){

        //list of file names
        ArrayList<String> fileNames = ClientHandler.handleListCommand();
        String [][] tableData = new String[fileNames.size()][TABLE_HEADER.length];
        DefaultTableModel model=new FileTableModel(fileNames.size(),TABLE_HEADER.length);
        int row = 0;
        for(String filename:fileNames){
            //get meta-data for the file
            FileMetaData meta=ClientHandler.handleMeta(("meta "+filename).split(" "));
            tableData[row] = meta.toMinimalArray();
            row++;
        }
        //set the table model
        model.setDataVector(tableData,TABLE_HEADER);
        file_table.setModel(model);
        //table = new JTable(model);
        scrollPane.setViewportView(file_table);
        //return table;
    }

    /**
     * Delete a file marked in the table
     */
    private void deleteMarkedFile(){
        int selectedRow=file_table.getSelectedRow();

        if(selectedRow!=-1 && JOptionPane.showConfirmDialog(null,"Are you sure you want to delete?") == JOptionPane.YES_OPTION){
            String filename = (String) file_table.getValueAt(selectedRow,0);
            boolean success = ClientHandler.handleDelete(("delete "+filename).split(" "));
            if(success){
                updateFileTable();
            }
        }

    }
    /**
     * Dispose and exit
     */
    private void onExit(){
        // TODO: writeMFileOnServer NOT HERE! in case of exiting the client illegaly, the file won't update.
        ClientHandler.writeMFileOnServer();
        mainFrame.dispose();
    }

    /**
     * A listener that updates the table on file deletion
     */
   public class DeleteFileActionListener implements ActionListener{

       @Override
       public void actionPerformed(ActionEvent e) {
           deleteMarkedFile();
       }
   }
}
