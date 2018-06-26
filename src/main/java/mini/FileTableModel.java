package mini;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/**
 * Extends DefaultTableModel
 * For creating a custom Table model, matching the needed table structure
 */
public class FileTableModel extends DefaultTableModel {

    public FileTableModel(int rowCount, int columnCount) {
        super(rowCount, columnCount);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if(column == 0)
            return true;
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        Object oldValue=getValueAt(row,column); //keep old value

        //change table value ONLY if possible to change name at server
        if(ClientHandler.handleRename(AuxFunctions.parseCommand("rename " + AuxFunctions.quotify((String) oldValue)+ " "+AuxFunctions.quotify((String) aValue))))
            super.setValueAt(aValue, row, column);

    }
}
