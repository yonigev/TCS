package mini;

import java.util.ArrayList;

public class AuxFunctions {

    /**
     * Removes all occurences of " " from a String array
     * @param strings
     * @return
     */
    public static String[] removeSpaces(String[] strings){
        ArrayList<String> arrayList = new ArrayList<>();
        for(String s: strings){
            arrayList.add(s);
        }
        while(arrayList.contains(" ")) { arrayList.remove(" ");}

        String[] toReturn = new String [arrayList.size()];
        for(int i=0; i<arrayList.size(); i++){
            toReturn[i]=arrayList.get(i);
        }
        return toReturn;
    }


    /**
     * Adds " " around a string - used when handling paths
     * @param string
     * @return
     */
    public static String quotify(String string){
        return "\""+string+"\"";
    }
}
