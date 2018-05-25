package mini;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTPClient;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.Scanner;
import org.apache.commons.io.IOUtils;

public class ClientHandler {
    private static final String FILE_DELETION_SUCCESS="File Successfully Deleted : ";
    private static final String FILE_DELETION_FAILURE="File Could not be Deleted : ";
    private static final String FILE_OVERWRITE_PROMPT="File already exists. overwrite? y/n";
    private static final String FILE_RENAME_ILLEGAL="Illegal Number of Arguments ";
    private String key1ForEncryption;
    private String key2ForAuthen;
    private String key3ForPassword;

    ClientHandler(String key1ForEncryption, String key2ForAuthen, String key3ForPassword){
            this.key1ForEncryption=key1ForEncryption;
            this.key2ForAuthen=key2ForAuthen;
            this.key3ForPassword= key3ForPassword;
    }

    /**
     * Handles the connection to the server
     * @param client
     */
    public void handleConnection(FTPClient client) {

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
                byte[] toSend = IOUtils.toByteArray( new FileInputStream(file));
            } catch (IOException e) {
                e.printStackTrace();
            }







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
     * encrypt data with encryptionKey using cipher with AES algorithm.
     * returning the encrypted data as string or null if fails.
     *
     * @param data
     * @param encryptionKey
     * @return
     */
    private static String encryptData(String data, String encryptionKey) {
        try {
            Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return new String(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Decrypt encrypted data with encryptionKey using AES and Cipher.
     * returning original data.
     *
     * @param encryptedData
     * @param encryptionKey
     * @return
     */
    private static String decryptData(String encryptedData, String encryptionKey) {
        try {
            Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return new String(cipher.doFinal(encryptedData.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * creating tag using Mac with authenKey on data.
     * returning the tag as string or null if fails.
     *
     * @param data
     * @param authenKey
     * @return
     */
    private static String getAuthenticationTag(String data, String authenKey) {
        try {
            SecretKeySpec macKey = new SecretKeySpec(authenKey.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(macKey);
            byte[] tag = mac.doFinal(data.getBytes());
            return new String(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    private static String authenticateData(String data, String authenKey) {
//
//    }
}
