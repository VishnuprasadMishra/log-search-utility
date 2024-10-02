package com.util.log.ui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * 
 * @author Vishnu Mishra
 *
 */
public class WordWrapRenderer extends JTextArea implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

	public WordWrapRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setEditable(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value != null ? value.toString() : "");
        setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
        if (table.getRowHeight(row) != getPreferredSize().height) {
            table.setRowHeight(row, getPreferredSize().height);
        }
        return this;
    }
}

class WordWrapEditor extends DefaultCellEditor {
    private JTextArea textArea;

    public WordWrapEditor() {
        super(new JTextField());
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        return textArea;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }
}


class LogFileFilter extends FileFilter {

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".log");
    }

    @Override
    public String getDescription() {
        return "Log Files (*.log)";
    }
}