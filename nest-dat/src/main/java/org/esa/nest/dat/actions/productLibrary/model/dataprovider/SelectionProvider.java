package org.esa.nest.dat.actions.productLibrary.model.dataprovider;

import org.esa.nest.db.ProductEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Comparator;

public class SelectionProvider implements DataProvider {

    private final Comparator _selectionComparator = new SelectionComparator();
    private TableColumn _selectionColumn = null;

    public boolean mustCreateData(final ProductEntry entry) {
        return false;
    }

    public void createData(final ProductEntry entry) throws IOException {
    }

    public Object getData(final ProductEntry entry) throws IOException {
        return entry;
    }

    public Comparator getComparator() {
        return _selectionComparator;
    }

    public void cleanUp(final ProductEntry entry) {
    }

    public TableColumn getTableColumn() {
        if(_selectionColumn == null) {
            _selectionColumn = new TableColumn();
            _selectionColumn.setHeaderValue("Select");
            _selectionColumn.setPreferredWidth(45);
            _selectionColumn.setResizable(false);
            _selectionColumn.setCellRenderer(new SelectionCellRenderer());
            _selectionColumn.setCellEditor(new CheckBoxCellEditor());
        }
        return _selectionColumn;
    }

    private static class SelectionCellRenderer extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        SelectionCellRenderer() {
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }

        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            final ProductEntry entry = (ProductEntry) value;
            if(entry != null)
                checkBox.setSelected(entry.isSelected());

            return checkBox;
        }
    }

    private static class CheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        final JCheckBox checkBox;
        private ProductEntry entry = null;

        public CheckBoxCellEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBackground(Color.white);

            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(entry != null)
                        entry.setSelected(checkBox.isSelected());
                }
            });
        }

        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {

            if (!(value instanceof ProductEntry)) {
                return null;
            }
            entry = (ProductEntry) value;

            checkBox.setSelected(entry.isSelected());

            final Component c = table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (c != null) {
                checkBox.setBackground(c.getBackground());
            }

            return checkBox;
        }
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }

    private static class SelectionComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if(o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if(o2 == null) {
                return 1;
            }

            final ProductEntry s1 = (ProductEntry) o1;
            final ProductEntry s2 = (ProductEntry) o2;

            if(s1.isSelected() && s2.isSelected())
                return 0;
            else if(s1.isSelected())
                return 1;
            else
                return -1;
        }
    }
}