package mini;

import java.util.ArrayList;
import java.util.Collections;

public class AuxFunctions {

    /**
     * Removes all occurences of " " from a String array
     * @param strings an array of Strings
     * @return the array without Spaces
     */
    private static String[] removeSpaces(String[] strings){
        ArrayList<String> arrayList = new ArrayList<>();
        Collections.addAll(arrayList, strings);
        while(arrayList.contains(" ")) { arrayList.remove(" ");}

        String[] toReturn = new String [arrayList.size()];
        for(int i=0; i<arrayList.size(); i++){
            toReturn[i]=arrayList.get(i);
        }
        return toReturn;
    }


    /**
     * Adds " " around a string - used when handling paths
     * @param string usually a file name
     * @return  the string with quotes around it
     */
    public static String quotify(String string){
        return "\""+string+"\"";
    }


    /**
     * Split an input with regex " \" " in case file names are involved
     * or split spaces if not
     * @param command the command to parse
     * @return the command, split into Strings
     */
    public static String[] parseCommand(String command){
        String[] splitCommand = command.split(" ");
        if(handlesAFile(splitCommand[0])){
            String[] paths = command.split("\"");
            return AuxFunctions.removeSpaces(paths);    //remove spaces - side effects of splitting with "
        }
        else
            return command.split(" ");
    }

    /**
     * Checks if an opcode represents a Command that handles file names (and should be aware of spaces
     * @param opcode the command opcode to check
     * @return True if the opcode represents a command that handles file names
     */
    private static boolean handlesAFile(String opcode){
        return (opcode.equals("write")|| opcode.equals("meta") || opcode.equals("delete")|| opcode.equals("rename"));
    }




    /**
     * Get file name from the Path
     *
     * @param path a path to a file
     * @return the file's name
     */
    static String getNameFromPath(String path) {
        if (path.contains("\\")) {
            String[] path_names = path.split("\\\\");
            return path_names[path_names.length - 1];
        } else
            return path;
    }

}
