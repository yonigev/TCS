package mini;

import java.util.Calendar;

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
}
