package mini;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents a file's Meta-data
 * used to organize the GUI Table more simply.
 */
public class FileMetaData {
    String filename;
    Calendar fileDateLastChanged;
    Long    filesize;

    public FileMetaData(String filename,Calendar fileDateLastChanged,Long filesize){
        this.filename=filename;
        this.fileDateLastChanged=fileDateLastChanged;
        this.filesize=filesize;
    }


    public String getFilename() {
        return filename;
    }

    public Calendar getFileDateLastChanged() {
        return fileDateLastChanged;
    }

    public Long getFilesize() {
        return filesize;
    }


    /**
     * @return return an array containing the pair:  size, date-modified
     */
    public String[] toArray(){
        String size = Long.toString(filesize)+ " bytes";
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        format.setTimeZone(TimeZone.getDefault());
        String date =  format.format(fileDateLastChanged.getTime());
        String[] toReturn=new String[3];
        toReturn[0] = filename;
        toReturn[1] = size;
        toReturn[2] = date;
        return toReturn;
    }



}
