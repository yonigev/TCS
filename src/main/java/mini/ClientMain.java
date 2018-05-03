package mini;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import sun.net.ftp.FtpClient;
import org.apache.commons.lang3.ArrayUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import java.io.*;
import java.net.Socket;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class ClientMain {
    private static final String DEFAULT_LOGIN = "anonymous";
    static String REGISTER_COMMAND = "USER !REGISTER!";
    private static final String ILLEGAL_INPUT = "Illegal Input!";
    private static final String LOGIN_PROMPT = "Welcome!\nLogin to Existing Account? (y/n):";
    protected static final String REGISTER_PROMPT = "Enter <username> <password> for the new user";
    private static final String USERNAME_PROMPT = "Enter your Username ";
    private static final String PASSWORD_PROMPT = "Enter your Password ";
    private static final String CONNECTION_ERROR = "Error! cannot connect to server ";
    private static final String FILE_DELETION_SUCCESS="File Successfully Deleted : ";
    private static final String FILE_DELETION_FAILURE="File Could not be Deleted : ";
    private static final String FILE_OVERWRITE_PROMPT="File already exists. overwrite? y/n";
    private static final String FILE_RENAME_ILLEGAL="Illegal Number of Arguments ";
    private static final int REGISTRATION_SUCCESS = 601;

    private static final String PASSWORD_SUFFIX_LOGIN="1";          //suffix added to the password for hashing - after hash- would be the user's password
    private static final String PASSWORD_SUFFIX_ENCRYPTION="2";     //suffix added to the password for hashing - after hash-would be used for file & file name encryption


    private static final Logger logger=Logger.getLogger("client_logger");
    private static MessageDigest digest;
    private static SecretKeyFactory secretKeyFactory;
    private static byte[] encryptionKey;

    public static File make_test_file() {
        File file = new File("test.txt");
        try {
            FileWriter fw = new FileWriter(file);

            fw.write("ABCDE");
            fw.flush();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * If user exists, login
     *
     * @param client
     * @param serverAddress
     * @throws IOException
     */
    private void tryConnectExistingUser(FTPClient client, String serverAddress) throws IOException {
        client.connect(serverAddress, 55555);

    }

    public static void main(String[] args) {
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("SHA256"); //TODO: READ ABOUT THIS.
            logger.info("secretKeyFactory: "+secretKeyFactory);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        FTPClient client = connectToServer("127.0.0.1");

        handleConnection(client);
    }

    /**
     * Connect and login to the FTP Server. (register to server if required.)
     *
     * @return the FTPClient
     */
    private static FTPClient connectToServer(String serverAddress) {
        FTPClient client = new FTPClient();
        try {
            System.out.println("CONNECTING....");
            client.connect(serverAddress, 44444);
            System.out.println("CONNECTED!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {

            while (true) {
                System.out.println(LOGIN_PROMPT);
                Scanner scanner = new Scanner(System.in);
                char input = scanner.next(".").charAt(0);
                if (input == 'y') {
                    while (!loginExistingAccount(client, scanner)) ;//try logging in until successful
                } else if (input == 'n') {
                    if (!registerNewAccount(client))         //register a new account
                        continue;
                    while (!loginExistingAccount(client, scanner)) ;//try logging in until successful
                }
                return client;
            }
        } else {
            System.out.println(CONNECTION_ERROR);
            return null;
        }

    }

    /**
     * Handles the connection to the server
     * @param client
     */
    private static void handleConnection(FTPClient client) {

        String input;
        String[] command;


        while (client.isConnected()) {
            Scanner sc = new Scanner(System.in);
            input = sc.nextLine();
            command = input.split(" ");
            if (command.length > 0) {
                switch (command[0]) {
                    case "ls":
                        handleListCommand(client);
                        break;
                    case "write":
                        handleWrite(client, command);
                        break;
                    case "read":
                        handleRead(client, command);
                        break;
                    case "delete":
                        handleDelete(client,command);
                    case "exit":
                        try {
                            client.logout();
                            client.disconnect();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "rename":
                        handleRename(client, command);
                        break;
                    default:
                        System.out.println("Unknown command");


                }


            } else {
                //TODO: COMMAND INPUT ERROR
            }


        }
    }

    /**
     * Handles the "rename" command
     * @param client
     * @param command
     */
    private static void handleRename(FTPClient client, String[] command) {
        if(command.length == 3){
            try {
                client.rename(command[1],command[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        else
            System.out.println(FILE_RENAME_ILLEGAL);
    }

    private static void handleDelete(FTPClient client, String[] command) {

        for (String name: command){
            if(name.equals(command[0])) //skip command name
                continue;
            try {
                if(client.deleteFile(name)){
                    System.out.print(FILE_DELETION_SUCCESS);
                    System.out.println(name);
                }
                else{
                    System.out.print(FILE_DELETION_FAILURE);
                    System.out.println(name);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private static void handleRead(FTPClient client, String[] command) {
        for (int i = 1; i < command.length; i++) {
            String name = command[i];
            File file = new File(name);

            try {
                if (!file.exists())
                    file.createNewFile();
                client.retrieveFile(name, new FileOutputStream(file));
                System.out.println(client.getReplyString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void handleWrite(FTPClient client, String[] command) {
        for (String filePath: command) {
            if(ArrayUtils.indexOf(command,filePath) == 0)
                continue;
            String path = filePath;
            File file = new File(path);
            try {
                //if file exists
                if(ArrayUtils.contains(client.listNames(),getNameFromPath(filePath))){
                    //ask user to overwrite
                    if(promptOverWrite(getNameFromPath(filePath))){
                        client.storeFile(getNameFromPath(path), new FileInputStream(file));
                        System.out.println(client.getReplyString());
                    }
                }
                else {
                    client.storeFile(getNameFromPath(path), new FileInputStream(file));
                    System.out.println(client.getReplyString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static boolean promptOverWrite(String name) {
        System.out.print(name+" : ");
        System.out.println(FILE_OVERWRITE_PROMPT);
        Scanner sc = new Scanner(System.in);
        char input = sc.next(".").charAt(0);
        return (input == 'y');

    }

    /**
     * Get file name from the Path
     *
     * @param path
     * @return
     */
    private static String getNameFromPath(String path) {
        if (path.contains("\\")) {
            String[] path_names = path.split("\\\\");
            return path_names[path_names.length - 1];
        } else
            return path;
    }

    /**
     * Handles an "ls" command
     *
     * @param client
     */
    private static void handleListCommand(FTPClient client) {
        try {
            String[] names = client.listNames();
            if (names != null && names.length > 0) {
                for (String fileName : names) {
                    System.out.println(fileName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Send the server a REGISTER USER command
     *
     * @param client
     */
    private static boolean registerNewAccount(FTPClient client) {
        //System.out.println(REGISTER_PROMPT);            //print a message to the user
        Scanner sc = new Scanner(System.in);
        String username;
        String password;
        System.out.println(USERNAME_PROMPT + "to register");
        username = sc.next();
        System.out.println(PASSWORD_PROMPT + "to register");
        password = sc.next();

        String commandToSend = REGISTER_COMMAND + " " + username + " " + encryptPassword(password,PASSWORD_SUFFIX_LOGIN);    //the command we will send to the server
        logger.info("Registering- "+commandToSend);
        logger.info("STAM BISHVIL LIROT:"+encryptPassword(password,PASSWORD_SUFFIX_LOGIN).toString());
        try {
            client.sendCommand(commandToSend);                              //send a registration request
            int reply = client.getReply();                                    //get a reply back from the server
            System.out.println(client.getReplyString());
            if (reply != REGISTRATION_SUCCESS) {                                //if NOT successful
                return false;
            } else
                return true;


        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] encryptPassword(String password,String con) {
        SecretKey key;
        KeySpec ks=new PBEKeySpec(password.toCharArray(),con.getBytes(),10,256);
        try {
            key=secretKeyFactory.generateSecret(ks);
            return key.getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Login to an Existing account. prompt the user for a Username and Password
     *
     * @param client
     * @param scanner
     * @return
     */
    private static boolean loginExistingAccount(FTPClient client, Scanner scanner) {
        String username;
        String password;
        System.out.println(USERNAME_PROMPT);
        while ((username = scanner.next()).length() == 0) {
            System.out.println(ILLEGAL_INPUT);
        }
        System.out.println(PASSWORD_PROMPT);
        while ((password = scanner.next()).length() == 0) {
            System.out.println(ILLEGAL_INPUT);
        }
        try {
            boolean success = client.login(username, encryptPassword(password,PASSWORD_SUFFIX_LOGIN).toString());
            logger.info("LOGIN with password "+encryptPassword(password,PASSWORD_SUFFIX_LOGIN).toString());
            if(success){
                    encryptionKey=encryptPassword(password,PASSWORD_SUFFIX_ENCRYPTION);
            }
            System.out.println(client.getReplyString());
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


    }


}
