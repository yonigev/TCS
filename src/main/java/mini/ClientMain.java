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
    private static final String DEFAULT_LOGIN = "anonymous";
    static String REGISTER_COMMAND = "USER !REGISTER!";
    private static final String ILLEGAL_INPUT = "Illegal Input!";
    private static final String LOGIN_PROMPT = "Welcome!\nLogin to Existing Account? (y/n):";
    protected static final String REGISTER_PROMPT = "Enter <username> <password> for the new user";
    private static final String USERNAME_PROMPT = "Enter your Username ";
    private static final String PASSWORD_PROMPT = "Enter your Password ";
    private static final String CONNECTION_ERROR = "Error! cannot connect to server ";

    private static final int REGISTRATION_SUCCESS = 601;

    private static final String PASSWORD_SUFFIX_ENCRYPTION = "1";           //suffix added to the password for hashing - after hash-would be used for file & file name encryption
    private static final String PASSWORD_SUFFIX_AUTHENTICATION = "2";      //suffix added to the password for hashing - after hash-would be used for authentication
    private static final String PASSWORD_SUFFIX_PASSWORD = "3";             //suffix added to the password for hashing - after hash- would be the user's password
    private static String key1ForEncryption;
    private static String key2ForAuthen;
    private static String key3ForPassword;

    private static final Logger logger = Logger.getLogger("client_logger");

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
//        try {
//            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); //TODO: READ ABOUT THIS.
//            logger.info("secretKeyFactory: "+secretKeyFactory);
//
//
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }

        FTPClient client = connectToServer("127.0.0.1");
        if (client != null) {
            ClientHandler handler = new ClientHandler(key1ForEncryption,key2ForAuthen,key3ForPassword);
            handler.handleConnection(client);
        }
        else
            logger.info("Error:: client got null , can't handle");
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

        deriveKeys(password);
        //String encryptedData = encryptData(key3ForPassword, key1ForEncryption);
        //String authenticationTag = getAuthenticationTag(key3ForPassword, key2ForAuthen);


        String commandToSend = REGISTER_COMMAND + " " + username + " " + key3ForPassword;   //the command we will send to the server
        //logger.info("Registering- " + commandToSend);
        //logger.info("STAM BISHVIL LIROT:" + key3ForPassword );
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
        deriveKeys(password);
        try {
            boolean success = client.login(username, key3ForPassword);
            // logger.info("LOGIN with password "+encryptPassword(password,PASSWORD_SUFFIX_LOGIN).toString());
//            if (success) {
//                encryptionKey = encryptPassword(password, PASSWORD_SUFFIX_ENCRYPTION);
//            }
            System.out.println(client.getReplyString());
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Setting up all 3 keys
     *
     * @param password
     */
    private static void deriveKeys(String password) {
        key1ForEncryption = DigestUtils.sha256Hex(password + PASSWORD_SUFFIX_ENCRYPTION);
        key2ForAuthen = DigestUtils.sha256Hex(password + PASSWORD_SUFFIX_AUTHENTICATION);
        key3ForPassword = DigestUtils.sha256Hex(password + PASSWORD_SUFFIX_PASSWORD);
    }

}
