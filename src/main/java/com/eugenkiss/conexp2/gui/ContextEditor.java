package com.eugenkiss.conexp2.gui;

import com.eugenkiss.conexp2.ProgramState;
import de.tudresden.inf.tcs.fcalib.FormalContext;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static com.eugenkiss.conexp2.gui.Util.createButton;

public class ContextEditor extends View {

    private static final long serialVersionUID = 1660117627650529212L;

    private final ContextMatrix matrix;
    private final ContextMatrixModel matrixModel;
    private final JButton addObjectButton;
    private final JButton clarifyObjectsButton;
    private final JButton reduceObjectsButton;
    private final JButton addAttributeButton;
    private final JButton clarifyAttributesButton;
    private final JButton reduceAttributesButton;
    private final JButton reduceContextButton;
    private final JButton transposeContextButton;

    public ContextEditor(final ProgramState state) {
        super(state);

        // Initialize components
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        matrixModel = new ContextMatrixModel(state.context);
        matrix = new ContextMatrix(matrixModel, panel.getBackground());
        Border margin = new EmptyBorder(1, 3, 1, 4);
        Border border = BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(220,220,220));
        JScrollPane scrollPane = ContextMatrix.createStripedJScrollPane(matrix, panel.getBackground());
        scrollPane.setBorder(border);
        toolbar.setFloatable(false);
        toolbar.setBorder(margin);
        panel.add(toolbar, BorderLayout.WEST);
        panel.add(scrollPane, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(panel);

        // Add buttons
        addObjectButton = createButton("Add Object", "addObject", "conexp/addObj.gif");
        toolbar.add(addObjectButton);
        clarifyObjectsButton = createButton("Clarify Objects", "clarifyObjects", "conexp/clarifyObj.gif");
        toolbar.add(clarifyObjectsButton);
        reduceObjectsButton = createButton("Reduce Objects", "reduceObjects", "conexp/reduceObj.gif");
        toolbar.add(reduceObjectsButton);
        toolbar.addSeparator();
        addAttributeButton = createButton("Add Attribute", "addAttribute", "conexp/addAttr.gif");
        toolbar.add(addAttributeButton);
        clarifyAttributesButton = createButton("Clarify Attributes", "clarifyAttributes", "conexp/clarifyAttr.gif");
        toolbar.add(clarifyAttributesButton);
        reduceAttributesButton = createButton("Reduce Attributes", "reduceAttributes", "conexp/reduceAttr.gif");
        toolbar.add(reduceAttributesButton);
        toolbar.addSeparator();
        reduceContextButton = createButton("Reduce Context", "reduceContext", "conexp/reduceCxt.gif");
        toolbar.add(reduceContextButton);
        transposeContextButton = createButton("Transpose Context", "transposeContext", "conexp/transpose.gif");
        toolbar.add(transposeContextButton);

        // Add actions
        transposeContextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                state.context.transpose();
                matrixModel.fireTableDataChanged();
            }
        });

        // Force an update of the table
        matrixModel.fireTableStructureChanged();
    }

}


/**
 * ContextMatrixModel allows the separation between the data and its presentation in the JTable.
 * Whenever the context is changed the changes are reflected (automatically) in the corresponding
 * JTable. In particular, if the user changes the context through the context editor what really
 * happens is that the context is changed (not the JTable per se) and the JTable is redrawn based
 * on the updated context.
 */
class ContextMatrixModel extends AbstractTableModel {

    private static final long serialVersionUID = -1509387655329719071L;

    private final FormalContext<String,String> context;

    ContextMatrixModel(FormalContext<String, String> context) {
        this.context = context;
    }

    @Override
    public int getRowCount() {
        return context.getObjectCount() + 1;
    }

    @Override
    public int getColumnCount() {
        return context.getAttributeCount() + 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0 && rowIndex == 0) {
            return "";
        }
        else if (columnIndex == 0) {
            return String.format("<html><div style='margin:2px 4px'><b>%s</b></div></html>",
                    context.getObjectAtIndex(rowIndex - 1).getIdentifier());
        }
        else if (rowIndex == 0) {
            return String.format("<html><div style='margin:2px 4px'><b>%s</b></div></html>",
                    context.getAttributeAtIndex(columnIndex - 1));
        }
        return context.objectHasAttribute(
                context.getObjectAtIndex(rowIndex - 1),
                context.getAttributeAtIndex(columnIndex - 1)) ? "X" : "";
    }

}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * ContextMatrix is simply a customisation of JTable in order to make it look & behave more
 * like a spreadsheet editor resp. ConExp's context editor. The code is intricate, a bit
 * ugly and uses quite a few snippets from various sources from the internet (see below).
 * That is just because of the way JTable is designed - it is not meant to be too flexible.
 *
 * Resources:
 * http://explodingpixels.wordpress.com/2009/05/18/creating-a-better-jtable/
 * http://stackoverflow.com/questions/14416188/jtable-how-to-get-selected-cells
 * http://stackoverflow.com/questions/5044222/how-can-i-determine-which-cell-in-a-jtable-was-selected?rq=1
*/
class ContextMatrix extends JTable {

    private static final long serialVersionUID = -7474568014425724962L;

    Color BACKGROUND_COLOR = Color.LIGHT_GRAY;
    private static final Color HEADER_COLOR = new Color(245, 245, 250);
    private static final Color EVEN_ROW_COLOR = new Color(252, 252, 252);
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color TABLE_GRID_COLOR = new Color(0xd9d9d9);

    public ContextMatrix(TableModel dm, Color bg) {
        super(dm);
        BACKGROUND_COLOR = bg;
        init();
    }

    private void init() {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setTableHeader(null);
        setOpaque(false);
        setGridColor(TABLE_GRID_COLOR);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setCellSelectionEnabled(true);
        setShowGrid(false);
    }

    private void alignCells() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    // Disallow header cells to be selected
    @Override
    public boolean isCellSelected(int i, int j) {
        return i != 0 && j != 0 && super.isCellSelected(i, j);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        alignCells();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
                                     int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        if (component instanceof JComponent) {
            ((JComponent)component).setOpaque(isCellSelected(row, column));
        }
        return component;
    }

    public static JScrollPane createStripedJScrollPane(JTable table, Color bg) {
        JScrollPane scrollPane =  new JScrollPane(table);
        scrollPane.setViewport(new StripedViewport(table, bg));
        scrollPane.getViewport().setView(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class StripedViewport extends JViewport {

        private static final long serialVersionUID = 171992496170114834L;

        private final Color BACKGROUND_COLOR;
        private final JTable fTable;

        public StripedViewport(JTable table, Color bg) {
            BACKGROUND_COLOR = bg;
            fTable = table;
            setBackground(BACKGROUND_COLOR);
            setOpaque(false);
            initListeners();
        }

        private void initListeners() {
            PropertyChangeListener listener = createTableColumnWidthListener();
            for (int i=0; i<fTable.getColumnModel().getColumnCount(); i++) {
                fTable.getColumnModel().getColumn(i).addPropertyChangeListener(listener);
            }
        }

        private PropertyChangeListener createTableColumnWidthListener() {
            return new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    repaint();
                }
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBackground(g);
            paintStripedBackground(g);
            paintVerticalHeaderBackground(g);
            paintHorizontalHeaderBackground(g);
            paintGridLines(g);
            super.paintComponent(g);
        }

        private void paintBackground(Graphics g) {
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(g.getClipBounds().x, g.getClipBounds().y,
                       g.getClipBounds().width, g.getClipBounds().height);
        }

        private void paintStripedBackground(Graphics g) {
            int rowHeight = fTable.getRowHeight();
            int tableWidth = fTable.getWidth();
            for (int j = 0; j < fTable.getRowCount(); j++) {
                g.setColor(j % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                g.fillRect(0, j * rowHeight, tableWidth, rowHeight);
            }
        }

        private void paintVerticalHeaderBackground(Graphics g) {
            int tableHeight = fTable.getHeight();
            int firstColumnWidth = fTable.getColumnModel().getColumn(0).getWidth();
            int rowHeight = fTable.getRowHeight();
            g.setColor(HEADER_COLOR);
            g.fillRect(0, 0, firstColumnWidth, tableHeight);

            g.setColor(new Color(255,255,255));
            g.drawLine(firstColumnWidth - 2, rowHeight, firstColumnWidth - 2, tableHeight);
            g.setColor(new Color(235,235,235));
            g.drawLine(firstColumnWidth, rowHeight, firstColumnWidth, tableHeight);
            for (int j = 0; j < fTable.getRowCount() + 1; j++) {
                g.setColor(new Color(255,255,255));
                g.drawLine(0, j * rowHeight - 1, firstColumnWidth - 1, j * rowHeight - 1);
            }
        }

        private void paintHorizontalHeaderBackground(Graphics g) {
            int tableWidth = fTable.getWidth();
            int firstRowHeight = fTable.getRowHeight();
            int columnWidth = fTable.getColumnModel().getColumn(0).getWidth();
            g.setColor(HEADER_COLOR);
            g.fillRect(0, 0, tableWidth, firstRowHeight);

            g.setColor(new Color(255, 255, 255));
            g.drawLine(columnWidth, firstRowHeight - 1, tableWidth, firstRowHeight - 1);
            g.setColor(new Color(235, 235, 235));
            g.drawLine(columnWidth, firstRowHeight+1, tableWidth, firstRowHeight+1);
            for (int j = 1; j < fTable.getColumnCount() + 1; j++) {
                g.setColor(new Color(255,255,255));
                g.drawLine(j * columnWidth - 2, 0, j * columnWidth - 2, firstRowHeight - 1);
            }
        }

        private void paintGridLines(Graphics g) {
            int tableWidth = fTable.getWidth();
            int rowHeight = fTable.getRowHeight();
            int columnHeight = rowHeight * fTable.getColumnCount();
            g.setColor(TABLE_GRID_COLOR);
            int x = 0;
            for (int i = 0; i < fTable.getColumnCount(); i++) {
                TableColumn column = fTable.getColumnModel().getColumn(i);
                x += column.getWidth();
                g.drawLine(x - 1, g.getClipBounds().y, x - 1, columnHeight);
            }
            for (int j = 1; j < fTable.getRowCount() + 1; j++) {
                g.drawLine(0, j * rowHeight, tableWidth - 1, j * rowHeight);
            }
        }

    }

}