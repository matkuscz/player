/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.Table;

import gui.GUI;
import gui.objects.TableRow.ImprovedTableRow;
import static java.lang.Math.floor;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.reactfx.Subscription;
import util.Util;

/**
 *
 * @author Plutonium_
 */
public class ImprovedTable<T> extends TableView<T> {
    
    final TableColumn<T,Void> columnIndex = new TableColumn("#");
    private final Callback<TableColumn<T,Void>, TableCell<T,Void>> indexCellFactory;
    boolean zero_pad = true;
    Subscription s;
    
    public ImprovedTable() {
        indexCellFactory = buildIndexColumnCellFactory();
        columnIndex.setCellFactory(indexCellFactory);
        columnIndex.setSortable(false);
        columnIndex.setResizable(false);
        getColumns().add(columnIndex);
    }
    
    /** Set visibility of columns header. Default true. */
    public void setHeaderVisible(boolean val) {
        if(val) getStylesheets().remove(PlaylistTable.class.getResource("Table.css").toExternalForm());
        else    getStylesheets().add(PlaylistTable.class.getResource("Table.css").toExternalForm());
    }
    
    /** @return visibility of columns header. Default true. */
    public boolean isTableHeaderVisible() {
        Pane header = (Pane)lookup("TableHeaderRow");
        return header==null ? true : header.isVisible();
    }
    
    /** @return height of columns header or 0 if invisible. */
    public double getTableHeaderHeight() {
        Pane header = (Pane)lookup("TableHeaderRow");
        return header==null ? getFixedCellSize() : header.getHeight();
    }
    
    /** Return index of a row containing the given y coordinate.
    Note: works only if table uses fixedCellHeight. */
    public int getRow(double y) {
        double h = isTableHeaderVisible() ? y - getTableHeaderHeight() : y;
        return (int)floor(h/getFixedCellSize());
    }
    
    /** Return index of a row containing the given scene y coordinate.
    Note: works only if table uses fixedCellHeight. */
    public int getRowS(double scenex, double sceney) {
            Point2D p = sceneToLocal(new Point2D(scenex,sceney));
            return getRow(p.getY());
    }
    
    /** Returns whether there is an item in the row at specified index */
    public boolean isRowFull(int i) {
        return 0<=i && getItems().size()>i;
    }
    
    /** Returns all tablerows using recursive lookup. Dont rely on this much ok. */
    public List<TableRow<T>> getRows() {
        return getRows(this, new ArrayList<>());
    }
    
    private List<TableRow<T>> getRows(Parent n, List<TableRow<T>> li) {
        for(Node nn : n.getChildrenUnmodifiable()) 
            if(nn instanceof TableRow)
                li.add(((TableRow)nn));
        for(Node nn : n.getChildrenUnmodifiable()) 
            if(nn instanceof Parent)
                getRows(((Parent)nn), li);
                
        return li;
    }
    
    public void updateStyleRules() {
        for (TableRow<T> row : getRows()) {
            if(row instanceof ImprovedTableRow) {
                ((ImprovedTableRow)row).styleRulesUpdate();
            }
        }
    }
    
    
    /** Returns selected items. The list will continue to reflect changes in selection. */
    public ObservableList<T> getSelectedItems() {
        return getSelectionModel().getSelectedItems();
    }
    
    /** @return unchanging copy of selected items. 
        @see #getSelectedItems() */
    public List<T> getSelectedItemsCopy() {
        return new ArrayList(getSelectionModel().getSelectedItems());
    }
    /** @return selected items or all if none selected. The list will continue
    to reflect change in selection or table list (depending on which was returned). */
    public ObservableList<T> getSelectedOrAllItems() {
        return getSelectionModel().isEmpty() ? getItems() : getSelectedItems();
    }
    /** @return unchanging copy of selected or all items.
        @see #getSelectedOrAllItems() */
    public List<T> getSelectedOrAllItemsCopy() {
        return new ArrayList(getSelectedOrAllItems());
    }
    
    /** Will add zeros to index numbers to maintain length consistency. */
    public void setZeropadIndex(boolean val) {
        zero_pad = val;
        refreshColumn(columnIndex);
    }
    
    /** @see #setZeropadIndex(boolean) */
    public boolean isZeropadIndex() {
        return zero_pad;
    }
    
    /** Max index. Normally equal to number of items. */
    public int getMaxIndex() {
        return getItems().size();
    }
    
    /** Refreshes given column. */
    public void refreshColumn(TableColumn c) {
        // c.setCellFactory(null);                      // this no longer works (since 8u40 ?)
        Callback cf = c.getCellFactory();               // use this
        c.setCellFactory(column->new TableCell());
        c.setCellFactory(cf);
    }
    
    /** Builds index column. */
    public TableColumn<T,Void> buildIndexColumn() {
        TableColumn<T,Void> c = new TableColumn("#");
                            c.setCellFactory(buildIndexColumnCellFactory());
                            c.setSortable(false);
                            c.setResizable(false);
        return c;
    }
    
    /** Builds index column cell factory. Called only once. */
    protected Callback<TableColumn<T,Void>, TableCell<T,Void>> buildIndexColumnCellFactory() {
        return (column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    int i = 1+getIndex();
                    setText((zero_pad ? Util.zeroPad(i, getItems().size(),'0') : i) + ".");
                }
            }
        });
    }
    
    /** Returns ideal width for index column derived from current max index.
        Mostly used during table/column resizing. */
    public double calculateIndexColumnWidth() {
        // need this weird method to get 9s as their are wide chars (font isnt 
        // always proportional)
        int s = getMaxIndex();
        int i = Util.decMin1(s);
        Text text = new Text();
             text.setFont(GUI.font.getValue());
             text.setText(i + ".");
             text.autosize();
             text.applyCss();
        double w = text.getLayoutBounds().getWidth() + 5;
        return w;
    }
    
/************************************ DRAG ************************************/
    
    /** 
     * Equivalent to {@link #setOnDragOver(javafx.event.EventHandler)}, but 
     * does nothing if the drag gesture source is this table. 
     * <p>
     * Drag over events should accept drag&drops (which is prevented), so other 
     * drag event setters need not this special handling. In effect drag from 
     * self to self is completely forbidden.
     * <p>
     * Note this works only when this table correctly assignes itself as the
     * drag source in the DRAG_DETECTED using {@link #startDragAndDrop(javafx.scene.input.TransferMode...)}
     */
    public void setOnDragOver_NoSelf(EventHandler<? super DragEvent> h) {
        setOnDragOver(e -> {
            if(e.getGestureSource()!=this)
                h.handle(e);
        });
    }
    
/************************************ SORT ************************************/
    
    /**
     * Sorts the items by the column. Sorting operates on table's sort
     * order and items backing the table remain unchanged. Sort order of 
     * the table is changed so specified column is primary sorting column and
     * other columns remain unaffected.
     * <p>
     * This is a programmatic equivalent of sorting the table manually by
     * clicking on columns' header (which operates through sort order).
     * <p>
     * Does not work when field's respective column is invisible - does nothing.
     * <p>
     * Note, that if the field must support sorting - return Comparable type.
     * 
     * @param field 
     */
    public void sortBy(TableColumn c, TableColumn.SortType type) {
        getSortOrder().remove(c);
        c.setSortType(type);
        getSortOrder().add(0, c);
    }
    
    
/************************************* HELPER *********************************/
    
    @Deprecated
    final void resizeIndexColumn() {
        getColumnResizePolicy().call(new ResizeFeatures(this, columnIndex, 0d));
    }
    
/***************************** OBSERVABLE VALUE *******************************/
    
    /** Minimalistic value wrapper for POJO table views cell value factories. */
    @Immutable
    public static class PojoV<T> implements ObservableValue<T> {
        private final T v;
        
        public PojoV(T v) {
            this.v = v;
        }

        @Override
        public void addListener(ChangeListener<? super T> listener) {}

        @Override
        public void removeListener(ChangeListener<? super T> listener) {}

        @Override
        public T getValue() {
            return v;
        }

        @Override
        public void addListener(InvalidationListener listener) {}

        @Override
        public void removeListener(InvalidationListener listener) {}
        
    }
    
}
