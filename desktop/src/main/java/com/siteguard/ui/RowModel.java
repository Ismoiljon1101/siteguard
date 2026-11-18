package com.siteguard.ui;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/** A simple table model over precomputed display rows. Values are already formatted strings or Tagged cells. */
final class RowModel extends AbstractTableModel {
    private final String[] columns;
    private List<Object[]> rows = List.of();

    RowModel(String... columns) {
        this.columns = columns.clone();
    }

    void setRows(List<Object[]> next) {
        rows = List.copyOf(next);
        fireTableDataChanged();
    }

    Object[] rowAt(int index) { return rows.get(index); }

    @Override public int getRowCount() { return rows.size(); }

    @Override public int getColumnCount() { return columns.length; }

    @Override public String getColumnName(int column) { return columns[column]; }

    @Override public Object getValueAt(int rowIndex, int column) { return rows.get(rowIndex)[column]; }
}
