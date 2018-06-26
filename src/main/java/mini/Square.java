package mini;

import javafx.stage.FileChooser;
import net.iharder.dnd.FileDrop;
import net.iharder.dnd.FileDropEvent;
import net.iharder.dnd.FileDropListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import sun.swing.ImageIconUIResource;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.FileChooserUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;



/**
 * The main class for GUI (Swing based) Client.
 */
public class Square extends ClientMain {
    public static final String WRITE_OPCODE = "write ";
    private static final String[] TABLE_HEADER = {"Name ", "Size ", "Last Modified "};
    FileDrop fileDrop;
    FileDrop.Listener fileDropListener;
    private JPanel back;
    private JTable file_table;
    private JScrollPane scrollPane;
    private JButton buttonDelete;
    private JButton buttonExit;
    private JButton saveAsButton;
    private JButton uploadButton;
    private static JFrame mainFrame;
    private DeleteFileActionListener deleteFileActionListener;
    private SaveButtonListener saveButtonListener;
    private UploadButtonListener uploadButtonListener;
    private static ImageIcon icon;
    private static TrayIcon trayIcon;
    final static SystemTray tray = SystemTray.getSystemTray();
    static boolean connected = false;
    private static final String SERVER_IP="127.0.0.1";

    public Square() {

        setGuiLook();
        URL imgURL=getClass().getResource("icon.png");
        if(imgURL!=null)
            icon=new ImageIcon(imgURL);

        ClientMain.GUI_ENABLED = true;               //for when prompting the user (like overwrite)
        setSystemTray();
        connected = GUI_connectToServer(SERVER_IP);
        if(!connected){
            JOptionPane.showMessageDialog(null,"Could not connect");
        }
        deleteFileActionListener = new DeleteFileActionListener();       //set listeners
        fileDropListener = new MyFileDropListener();                     //
        saveButtonListener = new SaveButtonListener();                   //
        uploadButtonListener=new UploadButtonListener();                 //
        fileDrop = new FileDrop(back, fileDropListener);                 //
        buttonExit.addActionListener(new ActionListener() {              //
            @Override
            public void actionPerformed(ActionEvent e) {
                onExit();
            }
        });
        buttonDelete.addActionListener(deleteFileActionListener);        //
        saveAsButton.addActionListener(saveButtonListener);              //
        uploadButton.addActionListener(uploadButtonListener);            //
        updateFileTable();                                               //update the file table to match the server's contents



    }

    /**
     * Logout and prompt login with another user
     */
    private void doSwitchUser(){
        onExit();
        try {
            if(!client.logout()){
                System.out.println("Could not log out");
                return;
            }
            client.disconnect();
            setFrameParameters();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the parameters for the JFrame
     * and set to Visible.
     */
    private static void setFrameParameters(){
        mainFrame.setContentPane(new Square().back);    //set "back" as the content
        if(icon!=null && icon.getImage()!=null)
            mainFrame.setIconImage(icon.getImage());
        mainFrame.setTitle("Encrypted Client");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setSize(400, 400);
        mainFrame.setVisible(true);
    }

    /**
     * Set the System tray icon and Menu
     */
    private void setSystemTray(){
        //if the icon was found, and no errors made
        if(icon!=null && icon.getImage()!=null) {
            trayIcon = new TrayIcon(icon.getImage());
            PopupMenu popupMenu = new PopupMenu();
            MenuItem exit = new MenuItem("Exit");
            MenuItem switchUser = new MenuItem("Switch User");
            exit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onExit();
                }
            });
            switchUser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doSwitchUser();
                }
            });
            popupMenu.add(switchUser);
            popupMenu.add(exit);
            trayIcon.setPopupMenu(popupMenu);

            try {

                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);

            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Set the UI to look like a normal Windows form (if on windows)
     */
    private void setGuiLook() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        mainFrame = new JFrame("Square");      //create new JFrame
       setFrameParameters();
    }

    /**
     * Connect to server using the UI instead of the CLI
     * @param serverAddress the server's IP Address
     * @return  true if connected successfully, false otherwise
     */
    protected boolean GUI_connectToServer(String serverAddress) {
        try {
            client.connect(serverAddress, 44444);       //connect to server
            client.setFileType(FTP.BINARY_FILE_TYPE); //set file type as Binary TODO:changed here from FTPClient.BINARY ...
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {
            LoginRegister loginRegister = new LoginRegister();
            loginRegister.setSize(500, 300);
            loginRegister.setResizable(false);
            loginRegister.setVisible(true);
            if (!loginRegister.isSuccessful())
                onExit();

        }
        return true;
    }

    /**
     * Create the JScrollPane for the table
     */
    private void createUIComponents() {
        //create the JScrollPane
        scrollPane = new JScrollPane(file_table);
    }

    /**
     * Update the file JList according to the server's contents.
     */
    private void updateFileTable() {

        //list of file names
        ArrayList<String> fileNames = ClientHandler.handleListCommand();
        String[][] tableData = new String[fileNames.size()][TABLE_HEADER.length];
        DefaultTableModel model = new FileTableModel(fileNames.size(), TABLE_HEADER.length);
        int row = 0;
        for (String filename : fileNames) {
            //get meta-data for the file
            FileMetaData meta = ClientHandler.handleMeta(AuxFunctions.parseCommand("meta " + AuxFunctions.quotify(filename)));
            tableData[row] = meta.toArray();
            row++;
        }
        //set the table model
        model.setDataVector(tableData, TABLE_HEADER);
        model.fireTableDataChanged();
        file_table.setModel(model);

        scrollPane.setViewportView(file_table);
        //return table;
    }

    /**
     * Delete a file marked in the table
     */
    private void deleteMarkedFile() {
        int selectedRow = file_table.getSelectedRow();
        int[] selectedRows=file_table.getSelectedRows();
        boolean success = false;
        if (selectedRows != null && JOptionPane.showConfirmDialog(null, "Are you sure you want to delete?") == JOptionPane.YES_OPTION) {
            for(int row : selectedRows) {
                String filename = (String) file_table.getValueAt(row, 0);
                if(ClientHandler.handleDelete(AuxFunctions.parseCommand("delete " + AuxFunctions.quotify(filename))))
                    success=true;
            }
            if (success) {
                updateFileTable();
            }
        }

    }

    /**
     * Dispose and exit
     */
    private void onExit() {
        //ClientHandler.writeMFileOnServer();
        tray.remove(trayIcon);
        mainFrame.dispose();
        Square.emergencyExit();
    }

    /**
     * "Hard" exit when files on server are illegally changed
     */
    public static void emergencyExit(){
        tray.remove(trayIcon);
            System.exit(0);
    }


    /**
     * A listener that updates the table on file deletion
     */
    private class DeleteFileActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            deleteMarkedFile();
        }
    }
    /**
     * On File Drop listener
     */
    private class MyFileDropListener implements FileDrop.Listener {

        @Override
        public void filesDropped(File[] files) {

            for (File f : files) {

                if (client.isConnected()) {
                    ClientHandler.handleWrite(AuxFunctions.parseCommand(WRITE_OPCODE + AuxFunctions.quotify(f.getPath())));
                    updateFileTable();
                }
            }
        }
    }
    /**
     * Listener called when "Save As" button is clicked
     */
    private class SaveButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();

            fileChooser.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
            fileChooser.showSaveDialog(null);
            File dir = fileChooser.getSelectedFile();   //get the chosen file\directory

            if (dir != null) {
                String toSave = (String) file_table.getValueAt(file_table.getSelectedRow(), 0);
                try {
                    byte[] fileBytes = ClientHandler.readFileToRAM(toSave);
                    if(fileBytes == null){
                        JOptionPane.showMessageDialog(null,ClientMain.FILE_DAMAGED_ERROR);
                        emergencyExit();
                    }
                    FileUtils.writeByteArrayToFile(new File(dir.getPath() + "\\" + toSave), fileBytes);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
    }

    /**
     * Listener calld when "Upload"  button is clicked
     */
    private class UploadButtonListener implements ActionListener {

         @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fileChooser.showOpenDialog(null);
            File file = fileChooser.getSelectedFile();   //get the chosen file\directory
            if(file!=null){
                String path=file.getPath();
                ClientHandler.handleWrite(AuxFunctions.parseCommand("write "+AuxFunctions.quotify(path)));
                updateFileTable();
            }

        }

    }
}
