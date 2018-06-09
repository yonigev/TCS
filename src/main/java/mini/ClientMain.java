package mini;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.ftp.FTPClient;

import java.security.Key;
import java.security.MessageDigest;

import java.io.*;
import java.util.Scanner;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class ClientMain {
    protected static String REGISTER_COMMAND = "USER !REGISTER!";
    protected static final String ILLEGAL_INPUT = "Illegal Input!";
    protected static final String LOGIN_PROMPT = "Welcome!\nLogin to Existing Account? (y/n):";
    protected static final String REGISTER_PROMPT = "Enter <username> <password> for the new user";
    protected static final String USERNAME_PROMPT = "Enter your Username ";
    protected static final String PASSWORD_PROMPT = "Enter your Password ";
    protected static final String CONNECTION_ERROR = "Error! cannot connect to server ";

    protected static final int REGISTRATION_SUCCESS = 601;

    protected static final String PASSWORD_SUFFIX_ENCRYPTION = "1";           //suffix added to the password for hashing - after hash-would be used for file & file name encryption
    protected static final String PASSWORD_SUFFIX_AUTHENTICATION = "2";      //suffix added to the password for hashing - after hash-would be used for authentication
    protected static final String PASSWORD_SUFFIX_PASSWORD = "3";             //suffix added to the password for hashing - after hash- would be the user's password
    protected static byte[] key1ForEncryption;
    protected static byte[] key2ForAuthen;
    protected static String key3ForPassword;

    protected static final Logger logger = Logger.getLogger("client_logger");



    public static void main(String[] args) {
        FTPClient client = connectToServer("127.0.0.1"); //TODO: change IP .
        if (client != null) {
            ClientHandler handler = new ClientHandler(client,key1ForEncryption,key2ForAuthen);
            handler.handleConnection();
        }
        else
            logger.info("Error:: client got null , can't handle");
    }

    /**
     * Connect and login to the FTP Server. (register to server if required.
     * @return the FTPClient
     */
    protected static FTPClient connectToServer(String serverAddress) {
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
     * Send the server a REGISTER USER command
     *
     * @param client
     */
    protected static boolean registerNewAccount(FTPClient client) {
        System.out.println(REGISTER_PROMPT);            //print a message to the user
        Scanner sc = new Scanner(System.in);
        String username;
        String password;
        System.out.println(USERNAME_PROMPT + "to register");
        username = sc.next();
        System.out.println(PASSWORD_PROMPT + "to register");
        password = sc.next();
        deriveKeys(password);
        String commandToSend = REGISTER_COMMAND + " " + username + " " + key3ForPassword;   //the command we will send to the server
        try {
            client.sendCommand(commandToSend);                              //send a registration request
            int reply = client.getReply();                                    //get a reply back from the server
            System.out.println(client.getReplyString());
            if (reply != REGISTRATION_SUCCESS) {                                //if NOT successful
                return false;
            } else{
                ClientHandler.writeMFileOnServer();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send the server a REGISTER USER command
     *
     * @param client
     */
    protected static boolean GUI_registerNewAccount(String username, String password,FTPClient client) {

        deriveKeys(password);
        String commandToSend = REGISTER_COMMAND + " " + username + " " + key3ForPassword;   //the command we will send to the server
        try {
            client.sendCommand(commandToSend);                              //send a registration request
            int reply = client.getReply();                                    //get a reply back from the server
            System.out.println(client.getReplyString());
            if (reply != REGISTRATION_SUCCESS) {                                //if NOT successful
                return false;
            } else {
                ClientHandler.writeMFileOnServer();
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean GUI_loginExistingAccount(FTPClient client, String username, String password) {
        deriveKeys(password);
        if(!ClientHandler.authenticateMFileData()) {
            System.out.println("Management File Damaged");
            return false;
        }
        try {
            boolean success_login = client.login(username, key3ForPassword);
            System.out.println(client.getReplyString());
            return success_login;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


    }

    /**
     * Login to an Existing account. prompt the user for a Username and Password
     *
     * @param client
     * @param scanner
     * @return
     */
    protected static boolean loginExistingAccount(FTPClient client, Scanner scanner) {
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
        deriveKeys(password);
        if(!ClientHandler.authenticateMFileData()) {
            System.out.println("Management File Damaged");
            return false;
        }
        try {
            boolean success = client.login(username, key3ForPassword);
            System.out.println(client.getReplyString());
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Setting up all 3 keys
     * @param password
     */
    protected static void deriveKeys(String password) {
        key1ForEncryption = DigestUtils.sha256(password + PASSWORD_SUFFIX_ENCRYPTION);
        key2ForAuthen = DigestUtils.sha256(password + PASSWORD_SUFFIX_AUTHENTICATION);
        key3ForPassword = DigestUtils.sha256Hex(password + PASSWORD_SUFFIX_PASSWORD);
    }

}
