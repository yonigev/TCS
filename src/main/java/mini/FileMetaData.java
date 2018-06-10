package mini;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

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
     * @return return an array containing <size, date-modified>
     */
    public String[] toMinimalArray(){
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
