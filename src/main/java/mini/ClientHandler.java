package mini;

import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.net.ftp.FTPClient;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPFile;

public class ClientHandler {
    protected static final String FILE_DELETION_SUCCESS = "File Successfully Deleted : ";
    protected static final String FILE_DELETION_FAILURE = "File Could not be Deleted : ";
    protected static final String FILE_OVERWRITE_PROMPT = "File already exists. overwrite? y/n";
    protected static final String FILE_RENAME_ILLEGAL = "Illegal Number of Arguments ";
    protected static final String MFILE_NAME = "nothing_important_here";
    protected static final Logger logger = Logger.getLogger("clientHandler_logger");
    protected static byte[] key1ForEncryption;
    protected static byte[] key2ForAuthen;
    protected static FTPClient client;

    ClientHandler(FTPClient client, byte[] key1ForEncryption, byte[] key2ForAuthen) {
        this.client = client;
        this.key1ForEncryption = key1ForEncryption;
        this.key2ForAuthen = key2ForAuthen;
        try {
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the connection to the server
     */
    public void handleConnection() {

        String input;
        String[] command;

        while (client.isConnected()) {
            Scanner sc = new Scanner(System.in);
            input = sc.nextLine();
            command = input.split(" ");
            if (command.length > 0) {
                switch (command[0]) {
                    case "ls":
                        handleListCommand();
                        break;
                    case "meta":
                        handleMeta(command);
                        break;
                    case "write":
                        handleWrite(command);
                        writeMFileOnServer();
                        break;
                    case "read":
                        handleRead(command);
                        break;
                    case "delete":
                        handleDelete(command);
                        writeMFileOnServer();
                        break;
                    case "exit":
                        try {
                            client.logout();
                            client.disconnect();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "rename":
                        handleRename(command);
                        writeMFileOnServer();
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
     *
     * @param command
     */
    protected void handleRename(String[] command) {
        if (command.length == 3) {
            try {

                byte[] originEncTag = encryptAndTagName(command[1]);     //original file name - encrypted and tagged
                byte[] changeToEncTag = encryptAndTagName(command[2]);   //new file name -  same
                client.rename(Arrays.toString(originEncTag), Arrays.toString(changeToEncTag));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            System.out.println(FILE_RENAME_ILLEGAL);
    }

    protected void handleDelete(String[] command) {

        for (String name : command) {
            if (name.equals(command[0])) //skip command name
                continue;
            try {
                if (client.deleteFile(Arrays.toString(encryptAndTagName(name)))) {
                    System.out.print(FILE_DELETION_SUCCESS);
                    System.out.println(name);
                } else {
                    System.out.print(FILE_DELETION_FAILURE);
                    System.out.println(name);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    protected int handleRead(String[] command) {
        for (int i = 1; i < command.length; i++) {
            String name = command[i];
            File file = new File(name);

            try {
                if (!file.exists()) ;
                file.createNewFile();
                byte[] originFile;
                if ((originFile = readFileToRAM(name)) == null)
                    return -1;
                FileUtils.writeByteArrayToFile(file, originFile); // writing byte array to file
                System.out.println(client.getReplyString());
                return client.getReply();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    /**
     * Encrypt a file and add a tag (suffix) for authentication
     *
     * @param fis
     * @return a InputStream ready to be sent to server
     * @throws IOException
     */
    protected static InputStream encryptAndTagFile(InputStream fis) throws IOException {
        byte[] originBytesToWrite = IOUtils.toByteArray(fis);
        byte[] encryptedBytesToWrite = encryptData(originBytesToWrite, key1ForEncryption);
        byte[] tag = getAuthenticationTag(encryptedBytesToWrite, key2ForAuthen);
        //merging 2 bytes array encryptedBytesToWrite + tag
        byte[] bytesToWrite = new byte[encryptedBytesToWrite.length + tag.length];
        System.arraycopy(encryptedBytesToWrite, 0, bytesToWrite, 0, encryptedBytesToWrite.length);
        System.arraycopy(tag, 0, bytesToWrite, encryptedBytesToWrite.length, tag.length);
        fis.close();

        return new ByteArrayInputStream(bytesToWrite);
    }

    /**
     * Encrypt a file name
     *
     * @param originFileName
     * @return
     * @throws IOException
     */
    protected static byte[] encryptAndTagName(String originFileName) throws IOException {
        byte[] originBytes = originFileName.getBytes();
        byte[] encryptedNameBytes = encryptData(originBytes, key1ForEncryption);
        byte[] tag = getAuthenticationTag(encryptedNameBytes, key2ForAuthen);
        byte[] ready = new byte[encryptedNameBytes.length + tag.length];
        System.arraycopy(encryptedNameBytes, 0, ready, 0, encryptedNameBytes.length);
        System.arraycopy(tag, 0, ready, encryptedNameBytes.length, tag.length);
        return ready;
    }

    /**
     * Decrypt and Authenticate a file name
     *
     * @param encName
     * @return
     */
    protected String decryptAndAuthName(String encName) {
        String[] byteValues = encName.substring(1, encName.length() - 1).split(",");
        byte[] encAuthNameBytes = new byte[byteValues.length];
        for (int i = 0, len = encAuthNameBytes.length; i < len; i++) {
            encAuthNameBytes[i] = Byte.parseByte(byteValues[i].trim());
        }
        // byte[] encAuthNameBytes=encName.getBytes();
        byte[] encNameBytes = authenticateData(encAuthNameBytes, key2ForAuthen);
        if (encNameBytes == null) {
            System.out.println("File name Authentication failed");
            return null;
        }
        byte[] nameBytes = decryptData(encNameBytes, key1ForEncryption);
        if (nameBytes != null) {
            return new String(nameBytes);
        } else return null;
    }

    /**
     * Handles a write command
     *
     * @param command
     */
    protected static void handleWrite(String[] command) {
        for (String filePath : command) {
            if (ArrayUtils.indexOf(command, filePath) == 0)
                continue;
            String path = filePath;

            File file = new File(path);
            try {
                InputStream fis = new FileInputStream(file);
                byte[] encAuthFileName = encryptAndTagName(getNameFromPath(path));
                InputStream readyForWriting = encryptAndTagFile(fis);
                //if file exists
                if (ArrayUtils.contains(client.listNames(), Arrays.toString(encAuthFileName))) {
                    //ask user to overwrite
                    if (promptOverWrite(getNameFromPath(filePath))) {
                        client.storeFile(Arrays.toString(encAuthFileName), readyForWriting);

                        System.out.println(client.getReplyString());
                    }
                } else {
                    client.storeFile(Arrays.toString(encAuthFileName), readyForWriting);
                    System.out.println(client.getReplyString());
                }
                readyForWriting.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Prompts the user for overwriting a file
     *
     * @param name
     * @return
     */
    protected static boolean promptOverWrite(String name) {
        System.out.print(name + " : ");
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
    protected static String getNameFromPath(String path) {
        if (path.contains("\\")) {
            String[] path_names = path.split("\\\\");
            return path_names[path_names.length - 1];
        } else
            return path;
    }

    /**
     * Handles an "ls" command
     */
    protected void handleListCommand() {
        try {
            String[] names = client.listNames();
            if (names != null && names.length > 0) {
                for (String fileName : names) {
                    String decName = decryptAndAuthName(fileName);
                    if (decName == null)
                        return;
                    System.out.println(decName);
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
    protected static byte[] encryptData(byte[] data, byte[] encryptionKey) {
        try {
            Key aesKey = new SecretKeySpec(encryptionKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return cipher.doFinal(data);
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
    protected static byte[] decryptData(byte[] encryptedData, byte[] encryptionKey) {
        try {
            Key aesKey = new SecretKeySpec(encryptionKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * creating tag of length 256bit using Mac with authenKey on data.
     * returning the tag as string or null if fails.
     *
     * @param data
     * @param authenKey
     * @return
     */
    protected static byte[] getAuthenticationTag(byte[] data, byte[] authenKey) {
        try {
            SecretKeySpec macKey = new SecretKeySpec(authenKey, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(macKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * split the data to message and tag and checks if : tag = MAC(message,key).
     * if yes return message, else return null.
     *
     * @param data
     * @param authenKey
     * @return
     */
    protected static byte[] authenticateData(byte[] data, byte[] authenKey) {
        try {
            SecretKeySpec macKey = new SecretKeySpec(authenKey, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(macKey);
            int messageLength = data.length - 32; // 32 is the tag length in bytes = 256bit.
            byte[] message = new byte[messageLength];
            byte[] tag = new byte[32];
            System.arraycopy(data, 0, message, 0, messageLength);
            System.arraycopy(data, messageLength, tag, 0, tag.length);
            if (Arrays.equals(mac.doFinal(message), tag))
                return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Handles the meta command
     *
     * @param command
     * @return
     */
    protected void handleMeta(String[] command) {
        if (command.length < 2) {
            System.out.println("Wrong usage of the Meta command. ");
            return;
        }
        String filename = command[1];
        try {
            FTPFile[] allFiles = client.listFiles();
            String encFilename = Arrays.toString(encryptAndTagName(filename));  //the encrypted file name
            for (FTPFile file : allFiles) {
                if (file.getName().equals(encFilename)) {
                    String size = Long.toString(file.getSize()) + "Bytes";
                    Calendar dateModified = file.getTimestamp();
//                    String timezone=System.getProperty("user.timezone");
//                    dateModified.setTimeZone(TimeZone.getTimeZone(timezone));        //set as current time zone

                    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    format.setTimeZone(TimeZone.getDefault());

                    System.out.println("Name: " + filename + "\n" + "Size: " + size + "\n" + "Last Modified: " + format.format(dateModified.getTime()));


                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void writeMFileOnServer() {
        try {
            byte[] currentMetaData = getCurrentMetaData();
            byte[] encAuthFileName = encryptAndTagName(MFILE_NAME);
            InputStream in = new ByteArrayInputStream(currentMetaData);
            InputStream readyForWriting = encryptAndTagFile(in);
            client.storeFile(Arrays.toString(encAuthFileName), readyForWriting);
            System.out.println(client.getReplyString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static boolean authenticateMFileData() {
        try {
            //READ the file byte data to Client RAM (authenticated and decrypted)
            byte[] managementData = readFileToRAM(MFILE_NAME);
            if (managementData == null)
                return false;
            byte[] currentMetaData = getCurrentMetaData();
            //TODO: check if both current meta data and Management data is the same!!
            if (Arrays.equals(managementData, currentMetaData))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Get metadata of ALL files on server and return a byte array of this data.
     * TODO: check order of returned FTPFiles.
     * @return
     */
    private static byte[] getCurrentMetaData() {
        try {
            //list all files
            FTPFile[] files = client.listFiles();
            if(files != null){
                StringBuilder stringBuilder=new StringBuilder();
                for(FTPFile f: files){
                    //append file name
                    stringBuilder.append(f.getName()+",");
                    //append file size
                    stringBuilder.append(Long.toString(f.getSize())+",");
                    //append file modification date!
                    stringBuilder.append(Long.toString(f.getTimestamp().getTimeInMillis()));
                    //newline
                    stringBuilder.append('\n');
                }
                return stringBuilder.toString().getBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] readFileToRAM(String fileName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        client.retrieveFile(Arrays.toString(encryptAndTagName(fileName)), out);
        byte[] bytesRead = out.toByteArray();
        out.close();
        byte[] encryptedFile = authenticateData(bytesRead, key2ForAuthen);
        if (encryptedFile == null) {
            System.out.println("file damaged");
            return null;
        }
        return decryptData(encryptedFile, key1ForEncryption);

    }

}
