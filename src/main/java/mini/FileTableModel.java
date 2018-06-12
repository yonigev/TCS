package mini;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

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
}
