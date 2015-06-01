/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.ItemNode.TableFilterGenerator;
import GUI.objects.ContextMenu.CheckMenuItem;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import static javafx.application.Platform.runLater;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import static javafx.css.PseudoClass.getPseudoClass;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import static org.reactfx.EventStreams.changesOf;
import static util.Util.zeroPad;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.async.executor.FxTimer;
import static util.functional.Util.*;

/**
 * 
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Plutonium_
 */
@IsConfigurable("Table")
public class FilteredTable<T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends FieldedTable<T,F> {
    
    private final ObservableList<T> allitems = FXCollections.observableArrayList();
    private final FilteredList<T> filtereditems = new FilteredList(allitems);
    private final SortedList<T> sortedItems = new SortedList(filtereditems);
    public final TableFilterGenerator<T,F> searchBox;
    final VBox root = new VBox(this);
    private boolean show_original_index;
    
    /* Predicate that filters the table list. Null predicate will match all
     * items. The value reflects the filter generated by the user through the
     * {@link #searchBox}. Changing the predicate programmatically is possible,
     * however the searchBox will not react on the change. 
     */
    public final ObjectProperty<Predicate<? super T>> predicate = filtereditems.predicateProperty();
    
    /**
     * @param main_field field that will denote main column. Must not be null.
     * Also initializes {@link #searchField}.
     * 
     * @param main_field WIll be chosen as main and default search field
     */
    public FilteredTable(F main_field) {
        super((Class<F>)main_field.getClass());
        searchField = main_field;
        
        setItems(sortedItems);
        sortedItems.comparatorProperty().bind(comparatorProperty());
        VBox.setVgrow(this, ALWAYS);
        
        searchBox = new TableFilterGenerator(filtereditems, main_field);
        searchBox.getNode().setVisible(false);
        searchBox.getNode().addEventFilter(KEY_PRESSED, e -> {
            // ESC -> close filter
            if(e.getCode()==ESCAPE) {
                // clear & hide filter on single ESC
                // searchBox.clear();
                // setFilterVisible(false);
                
                // clear filter on 1st, hide on 2nd
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            }
            // CTRL+F -> hide filter
            if(e.getCode()==F && e.isShortcutDown()) {
                setFilterVisible(false);
                requestFocus();
            }
        });
        
        // using EventFilter would cause ignoring first key stroke when setting
        // search box visible
        addEventHandler(KEY_PRESSED, e -> {
            // CTRL+F -> toggle filter
            if(e.getCode()==F && e.isShortcutDown()) {
                setFilterVisible(!isFilterVisible());
                if(!isFilterVisible()) requestFocus();
                return;
            }
            
            if(e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
            KeyCode k = e.getCode();
            // ESC, filter not focused -> close filter
            if(k==ESCAPE) {
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            }
            
            // typing -> scroll to
            if (k.isDigitKey() || k.isLetterKey()){
                String st = e.getText().toLowerCase();
                // update scroll text
                long now = System.currentTimeMillis();
                boolean append = scrolFTime==-1 || now-scrolFTime<scrolFTimeMax;
                scrolFtext = append ? scrolFtext+st : st;
                scrolFTime = now;
                search(scrolFtext);
            }
        });
        addEventFilter(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE && !scrolFtext.isEmpty()) {
                searchEnd();
                e.consume(); // causes all KEY_PRESSED handlers to be ignored
            }
        });
        addEventFilter(Event.ANY, e -> updateSearchStyles());
        changesOf(getItems()).subscribe(c -> updateSearchStyles());
        
        // resize index column on change of filter & items
        changesOf(filtereditems.predicateProperty()).subscribe(c -> resizeIndexColumn());
        changesOf(allitems).subscribe(c -> resizeIndexColumn());

    }
    
    /** @return the table filter */
    public TableFilterGenerator<T,F> getSearchBox() {
        return searchBox;
    }
    
    /** The root is a container for this table and the filter. Use the root instead
     * of this table when attaching it to the scene graph.
     * @return the root of this table */
    public VBox getRoot() {
        return root;
    }
    
    /** Return the items assigned to this this table. Includes the filtered out items. 
     * <p>
     * This list can be modified, but it is recommended to use {@link #setItemsRaw(java.util.Collection)}
     * to change the items in the table.
     */
    public final ObservableList<T> getItemsRaw() {
        return allitems;
    }
    
    /**
     * Sets items to the table. If any filter is in effect, it will be applied.
     * <p>
     * Do not use {@link #setItems(javafx.collections.ObservableList)} or 
     * {@code getItems().setAll(new_items)}. It will cause the filters to stop
     * working. The first replaces the table item list (instance of {@link FilteredList},
     * which must not happen. The second would throw an exception as FilteredList
     * is not directly modifiable.
     * 
     * @param items 
     */
    public void setItemsRaw(Collection<T> items) {
        allitems.setAll(items);
    }
    
    public final boolean isFilterVisible() {
        return searchBox.getNode().isVisible();
    }
    public final void setFilterVisible(boolean v) {
        if(searchBox.getNode().isVisible()==v && v) runLater(searchBox::focus);
        if(searchBox.getNode().isVisible()==v) return;
        
        if(!v) searchBox.clear();
        
        if(v) root.getChildren().setAll(searchBox.getNode(),this);
        else root.getChildren().setAll(this);
        searchBox.getNode().setVisible(v);
        VBox.setVgrow(this, ALWAYS);
        
        // after gui changes, focus on filter so we type the search criteria
        if(v) runLater(searchBox::focus);
    }
    
    /**
     * Maps the index of this list's filtered element to an index in the direct source list.
     *
     * @param index the index in filtered list of items visible in the table 
     * @return index in the unfiltered list backing this table
     */
    public int getSourceIndex(int i) {
        return filtereditems.getSourceIndex(i);
    }
    
/********************************** INDEX *************************************/
    
    /** 
     * @param true shows item's index in the observable list - source of its
     * data. False will display index within filtered list. In other words false
     * will cause items to always be indexed from 1 to items.size. This has only
     * effect when filtering the table. 
     */
    public final void setShowOriginalIndex(boolean val) {
        show_original_index = val;
        refreshColumn(columnIndex);
    }
    
    public final boolean isShowOriginalIndex() {
        return show_original_index;
    }
    
    /** 
     * Indexes range from 1 to n, where n can differ when filter is applied.
     * Equivalent to: {@code isShowOriginalIndex ? getItemsRaw().size() : getItems().size(); }
     * @return max index */
    @Override
    public final int getMaxIndex() {
        return show_original_index ? allitems.size() : getItems().size();
    }
    
/************************************ SCROLL **********************************/
    
    public void scrollToCenter(int i) {
        int items = getItems().size();
        if(i<0 || i>=items) return;
        
        double rows = getHeight()/getFixedCellSize();
        i -= rows/2;
        i = min(items-(int)rows+1,max(0,i));
        scrollTo(i);
    }
    
/************************************ SEARCH **********************************/
    
    /** 
     * If the user types text to quick search content by scrolling table, the
     * text matching will be done by this field. Its column cell data must be
     * String (or search will be ignored) and column should be visible. 
     */
    private F searchField;
    private String scrolFtext = "";
    private long scrolFTime = -1;
    private static final PseudoClass searchmatchPC = getPseudoClass("searchmatch");
    private static final PseudoClass searchmatchnotPC = getPseudoClass("searchmatchnot");
    private final FxTimer scrolFautocancelTimer = new FxTimer(3000,-1,this::searchEnd);
    
    @IsConfig(name = "Search delay", info = "Maximal time delay (ms) between key strokes. Search text is reset after the delay runs out.")
    private static long scrolFTimeMax = 500;
    @IsConfig(name = "Search auto-cancel", info = "Deactivates search after period of inactivity.")
    private static boolean scrolFautocancel = true;
    @IsConfig(name = "Search auto-cancel delay", info = "Period of inactivity to end search after.")
    private static long scrolFautocancelTime = 3000;
    @IsConfig(name = "Search use contains", info = "Use 'contains' instead of 'starts with' for string matching.")
    private static boolean scrolFTimeMatchContain = true;

    /** Sets fields to be used in search. Default is main field. */
    public void searchSetColumn(F field) {
        searchField = field;
    }
    
    /** 
     * Returns whether search is active. Every search must be ended, either
     * automatically {@link #scrolFautocancel}, or manually {@link #searchEnd()}.
     */
    public boolean searchIsActive() {
        return !scrolFtext.isEmpty();
    }
    
    /** 
     * Starts search, searching for the specified string in the designated
     * column for field {@link #searchField} (column can be invisible).
     */
    public void search(String s) {
        scrolFtext = s;
        // scroll to first item beginning with search string
        TableColumn c = getColumn(searchField).orElse(null);
        if(!getItems().isEmpty() && c!=null && c.getCellData(0) instanceof String) {
            for(int i=0;i<getItems().size();i++) {
                String item = (String)getItems().get(i).getField(searchField);
                if(matches(item,scrolFtext)) {
                    scrollToCenter(i);
                    updateSearchStyles();
                    break;
                }
            }
        }
    }
    
    /**
     * Ends search manually.
     */
    public void searchEnd() {
        scrolFtext = "";
        updateSearchStyleRowsNoReset();
    }
    
    private void updateSearchStyles() {
        if(scrolFautocancel) scrolFautocancelTimer.restart(scrolFautocancelTime);
        updateSearchStyleRowsNoReset();
    }
    
    private void updateSearchStyleRowsNoReset() {
        boolean searchOn = searchIsActive();
        for (TableRow<T> row : getRows()) {
            T t = row.getItem();
            Object o = t==null ? null : t.getField(searchField); 
            boolean isMatch = o instanceof String && matches((String)o,scrolFtext);
            row.pseudoClassStateChanged(searchmatchPC, searchOn && isMatch);
            row.getChildrenUnmodifiable().forEach(c->c.pseudoClassStateChanged(searchmatchPC, searchOn && isMatch));
            row.pseudoClassStateChanged(searchmatchnotPC, searchOn && !isMatch);
            row.getChildrenUnmodifiable().forEach(c->c.pseudoClassStateChanged(searchmatchnotPC, searchOn && !isMatch));
       }
    }
    
    private boolean matches(String text, String s) {
        String x = s.toLowerCase();
        String in = text.toLowerCase();
        return scrolFTimeMatchContain ? in.contains(s) : in.startsWith(x);
    }

    
/************************************* SORT ***********************************/
    
    /** {@inheritDoc} */
    @Override
    public void sortBy(F field) {
        getSortOrder().clear();
        allitems.sort(by(p -> (Comparable) p.getField(field)));
    }
    
    /***
     * Sorts items using provided comparator. Any sort order is cleared.
     * @param comparator 
     */
    public void sort(Comparator<T> comparator) {
        getSortOrder().clear();
        allitems.sorted(comparator);
    }
    
/*********************************** HELPER ***********************************/
    
    @Override
    protected Callback<TableColumn<T, Void>, TableCell<T, Void>> buildIndexColumnCellFactory() {
        return ( column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override 
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setText(null);
                else {
                    int j = getIndex();
                    String txt;
                    if(zero_pad) {
                        int i = show_original_index ? filtereditems.getSourceIndex(j) : j;      // BUG HERE
                        txt = zeroPad(i+1, getMaxIndex(), '0');
                    } else
                        txt = String.valueOf(j+1);
                    
                    setText( txt + ".");
                }
            }
        });
    }

    @Override
    public TableColumnInfo getDefaultColumnInfo() {
        boolean b = columnVisibleMenu==null;
        TableColumnInfo tci = super.getDefaultColumnInfo();
        if(b) {
            Menu m = new Menu("Search column");
            List<MenuItem> mis = filterMap(getFields(),
                f-> isIn(f.getType(),String.class,Object.class), // objects too, they can be strings
                f -> {
                    CheckMenuItem mi = new CheckMenuItem(f.name(),f==searchField);
                    mi.setOnMouseClicked(() -> {
                        m.getItems().forEach(i -> ((CheckMenuItem)i).selected.set(false));
                        mi.selected.set(true);
                        searchField = f;
                    });
                    return mi;
                }
            );
            m.getItems().addAll(mis);
            columnVisibleMenu.getItems().add(m);            
        }
        return tci;
    }

}