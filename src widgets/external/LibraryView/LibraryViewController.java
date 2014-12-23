 
package LibraryView;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import AudioPlayer.tagging.MetadataGroup;
import static AudioPlayer.tagging.MetadataGroup.Field.VALUE;
import Configuration.Config;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.Table.FilteredTable;
import GUI.objects.Table.ImprovedTable;
import GUI.objects.Table.TableColumnInfo;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.INHERIT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.K;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.reactfx.Subscription;
import org.reactfx.util.Tuples;
import static util.Util.DEFAULT_ALIGNED_CELL_FACTORY;
import static util.Util.consumeOnSecondaryButton;
import static util.Util.createIndexColumn;
import static util.Util.createmenuItem;
import util.access.Accessor;
import util.access.AccessorEnum;
import util.collections.Histogram;
import util.collections.Tuple4;
import static util.functional.FunctUtil.find;
import static util.functional.FunctUtil.isFALSE;
import static util.functional.FunctUtil.list;
import static util.functional.FunctUtil.listM;

/**
 *
 * @author Plutonium_
 */
@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
//            "    Item drag : \n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
//            "    Type : Searches for item - applies filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n",
    notes = "",
    version = "0.6",
    year = "2014",
    group = LIBRARY
)
public class LibraryViewController extends FXMLController {
    
    private @FXML AnchorPane root;
    private @FXML VBox content;
    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);
    private Subscription dbMonitor;
    private final List<String> ALL_COLUMNS = getAll_columns();

    
    // configurables
    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
    public final Accessor<Boolean> show_menu_button = new Accessor<>(true, table::setTableMenuButtonVisible);
    @IsConfig(editable = false)
    private TableColumnInfo columnInfo = new TableColumnInfo(ALL_COLUMNS);
    @IsConfig(name = "Library level", info = "")
    public final Accessor<Integer> lvl = new Accessor<>(1, v -> {
        if(dbMonitor!=null) dbMonitor.unsubscribe();
        // listen for database changes to refresh library
        dbMonitor = DB.views.subscribe(v, (i,list) -> {
            setItems(list);
        });
        // initialize
        setItems(DB.views.getValue(v));
    });
    @IsConfig(name = "Field")
    public final AccessorEnum<Metadata.Field> fieldFilter = new AccessorEnum<Metadata.Field>(CATEGORY, v -> {
            // rebuild value column
            find(table.getColumns(), c -> VALUE == c.getUserData()).ifPresent(c -> {
                TableColumn<MetadataGroup,?> t = buildColumn(v.toStringEnum());
                c.setText(t.getText());
                c.setCellFactory((Callback)t.getCellFactory());
                c.setCellValueFactory((Callback)t.getCellValueFactory());
            });
            
            // update filters
            table.getSearchBox().setPrefTypeSupplier(() -> Tuples.t(VALUE.toString(v), VALUE.getType(v), VALUE));
            table.getSearchBox().setData(list(MetadataGroup.Field.values(),
                                              MetadataGroup.Field::isTypeStringRepresentable,
                                              mgf->Tuples.t(mgf.toString(v),mgf.getType(v),mgf)));
            // repopulate
            setItems(DB.views.getValue(lvl.getValue()));
        },
        ()->list(Metadata.Field.values(), Field::isTypeStringRepresentable)
    );
    
    
    private boolean init = false;
    private final Callback<String,TableColumn<MetadataGroup,?>> columnFactory = name -> {
        if("#".equals(name))
            return table.buildIndexColumn();
        else {
            MetadataGroup.Field f = MetadataGroup.Field.valueOfEnumString(name);
            Metadata.Field v = fieldFilter.getValue();
            TableColumn<MetadataGroup,?> c = new TableColumn(f.toString(v));
            c.setCellValueFactory( cf -> {
                if(cf.getValue()==null) return null;
                return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
            });
            String no_val = f==VALUE ? "<none>" : null;
            c.setCellFactory(DEFAULT_ALIGNED_CELL_FACTORY(f.getType(v), no_val));
            c.setUserData(f);
            return c;
        }
    };
    private TableColumn<MetadataGroup,?> buildColumn(String name) {
        return columnFactory.call(name);
    }
    

    @Override
    public void init() {
        content.getChildren().addAll(table.getRoot());
        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
        
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        
        // add index column
        TableColumn indexColumn = createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
        // context menu
        table.setOnMouseClicked( e -> {
            if (e.getY()<table.getFixedCellSize()) return;
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2)
                    play(table.getSelectedItemsCopy());
            } else
            if(e.getButton()==SECONDARY)
                contxt_menu.show(table, e);
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER)       // play first of the selected
                play(table.getSelectedItems());
            else if (e.getCode() == ESCAPE) // deselect
                table.getSelectionModel().clearSelection();
            else if (e.getCode() == K)      // layout columns
                resizeMainColumn();
        });
        
        // send selection changed events, do not use InvalidationListener
        table.getSelectionModel().getSelectedItems().addListener(
                (Observable o) -> forwardItems(DB.views.getValue(lvl.getValue())));
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
        
        // prevent overly eager selection change
        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
    }

    @Override
    public void refresh() {
        
        table_orient.applyValue();
        zeropad.applyValue();
        orig_index.applyValue();
        show_header.applyValue();
        show_menu_button.applyValue();
        fieldFilter.applyValue();
        // apply last as it loads data
        lvl.applyValue();
        
        // set up columns - run only once
        if(init) return;
        columnInfo.nameKeyMapper = name -> {
            if(name.equals("#")) return name;
            if(Stream.of(MetadataGroup.Field.values()).anyMatch(m->m.toStringEnum().equals(name)))
                return name;
            else return VALUE.toStringEnum();
        };
        List<TableColumn<MetadataGroup,?>> tmp = new ArrayList();
        columnInfo.columns.stream().sorted().forEach(c->{
            // get or build column
            TableColumn tc = null;
            for(TableColumn t : table.getColumns())
                if(c.name.equals(t.getText())) {
                    tc = t;
                    break;
                }
            if(tc==null) tc = columnFactory.call(c.name);
            
            // set width
            // bug, last column sets twice the width, so divide it
            double w = c.position==tmp.size()-1 ? c.width/2 : c.width;
            tc.setPrefWidth(w);
            // set visibility
            tc.setVisible(c.visible);
            // set position (automatically, because we sorted the input)
            tmp.add(tc);
        });
        // restore
        table.getColumns().setAll(tmp);
        init=true;
    }
    
    private List<String> getAll_columns() {
        List<String> l = new ArrayList();
                     l.add("#");
        l.addAll(listM(MetadataGroup.Field.values(), m->m.toStringEnum()));
        return l;
    }
    
    public void resizeMainColumn() {
        find(table.getColumns(),c -> VALUE == c.getUserData()).ifPresent(c->{
            double w = table.getColumns().stream().mapToDouble(TableColumn::getWidth).sum();
            double itemsHeight = (table.getItems().size()+1)*table.getFixedCellSize();
            double scrollbar = itemsHeight < table.getHeight() ? 0 : 15;
            c.setPrefWidth(table.getWidth()-(scrollbar+w-c.getWidth()));
        });
    }

    @Override
    public void close() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }

    @Override
    public List getFields() {
        // serialize column state as config
        columnInfo.update(table);
        return super.getFields(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Config getField(String name) {
        if("columnInfo".equals(name)) columnInfo.update(table);
        return super.getField(name); //To change body of generated methods, choose Tools | Templates.
    }
    
    
/******************************** PRIVATE API **********************************/
    
    private final Histogram<Object, Metadata, Tuple4<Long,Set<String>,Double,Long>> h = new Histogram();
    
    /** populates metadata groups to table from metadata list */
    private void setItems(List<Metadata> list) {
        Field f = fieldFilter.getValue();
        // make histogram
        h.keyMapper = metadata -> metadata.getField(f);
        h.histogramFactory = () -> new Tuple4(0l,new HashSet(),0d,0l);
        h.elementAccumulator = (hist,metadata) -> {
            hist.a++;
            hist.b.add(metadata.getAlbum());
            hist.c += metadata.getLengthInMs();
            hist.d += metadata.getFilesizeInB();
        };
        h.clear();
        h.accumulate(list);
        // read histogram
        table.setItemsRaw(h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d)));
        // pass down the chain
        forwardItems(list);
    }
    
    /** Sends event to next level. */
    private void forwardItems(List<Metadata> list) {
        List<Metadata> forwardList;
        if(table.getSelectionModel().isEmpty()) {
            // no selection -> fetch everything, this is simplified behavior as
            // filter might apply, in such case we should filter the items too
            // i fear of bad performance, say predicate compound of 2000 predicates or even more
            forwardList = list;
        } else {
            Predicate<Metadata> p = table.getSelectedItems().stream()
                    .map(MetadataGroup::toMetadataPredicate)
                    .reduce(Predicate::or)
                    .orElse(isFALSE);
            forwardList = list.stream().filter(p).collect(toList());
        }
        DB.views.push(lvl.getValue()+1, forwardList);
    }
    
/******************************** CONTEXT MENU ********************************/
    
    private static final TableContextMenuInstance<MetadataGroup> contxt_menu = new TableContextMenuInstance<>(
        () -> {
            ContentContextMenu<List<MetadataGroup>> m = new ContentContextMenu();
            
            m.getItems().addAll(
                createmenuItem("Play items", e -> play(m.getValue())),
                createmenuItem("Enqueue items", e -> PlaylistManager.addItems(dbFetch(m.getValue()))),
                createmenuItem("Update from file", e -> DB.updateItemsFromFile(dbFetch(m.getValue()))),
                createmenuItem("Remove from library", e -> DB.removeItems(dbFetch(m.getValue()))),
                createmenuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(dbFetch(m.getValue())))));
//                createmenuItem("Edit the item/s in tag editor", e -> m.getV);
            return m;
        },
        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
    );
    
    private static List<Metadata> dbFetch(List<MetadataGroup> filters) {
        return DB.getAllItemsWhere(filters.get(0).getField(), filters.get(0).getValue());
    }
    private static void play(List<MetadataGroup> filters) {
        if(filters.isEmpty()) return;
        List<PlaylistItem> to_play = new ArrayList();
        dbFetch(filters).stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
        PlaylistManager.playPlaylist(new Playlist(to_play));
    }
}
// 
//package LibraryView;
//
//import AudioPlayer.playlist.Playlist;
//import AudioPlayer.playlist.PlaylistItem;
//import AudioPlayer.playlist.PlaylistManager;
//import AudioPlayer.services.Database.DB;
//import AudioPlayer.tagging.Metadata;
//import AudioPlayer.tagging.Metadata.Field;
//import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
//import AudioPlayer.tagging.MetadataGroup;
//import static AudioPlayer.tagging.MetadataGroup.Field.VALUE;
//import Configuration.Config;
//import Configuration.IsConfig;
//import GUI.GUI;
//import GUI.objects.ContextMenu.ContentContextMenu;
//import GUI.objects.ContextMenu.TableContextMenuInstance;
//import GUI.objects.Table.FilteredTable;
//import GUI.objects.Table.ImprovedTable;
//import GUI.objects.Table.TableColumnInfo;
//import Layout.Widgets.FXMLController;
//import Layout.Widgets.Features.TaggingFeature;
//import static Layout.Widgets.Widget.Group.LIBRARY;
//import Layout.Widgets.Widget.Info;
//import Layout.Widgets.WidgetManager;
//import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.function.Predicate;
//import static java.util.stream.Collectors.toList;
//import java.util.stream.Stream;
//import javafx.beans.Observable;
//import javafx.beans.property.ReadOnlyObjectWrapper;
//import javafx.event.Event;
//import javafx.fxml.FXML;
//import javafx.geometry.NodeOrientation;
//import static javafx.geometry.NodeOrientation.INHERIT;
//import static javafx.scene.control.SelectionMode.MULTIPLE;
//import javafx.scene.control.TableColumn;
//import static javafx.scene.input.KeyCode.ENTER;
//import static javafx.scene.input.KeyCode.ESCAPE;
//import static javafx.scene.input.KeyCode.K;
//import static javafx.scene.input.MouseButton.PRIMARY;
//import static javafx.scene.input.MouseButton.SECONDARY;
//import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
//import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.VBox;
//import javafx.util.Callback;
//import org.reactfx.Subscription;
//import org.reactfx.util.Tuples;
//import static util.Util.DEFAULT_ALIGNED_CELL_FACTORY;
//import static util.Util.consumeOnSecondaryButton;
//import static util.Util.createIndexColumn;
//import static util.Util.createmenuItem;
//import util.access.Accessor;
//import util.access.AccessorEnum;
//import util.collections.Histogram;
//import util.collections.Tuple4;
//import static util.functional.FunctUtil.*;
//
///**
// *
// * @author Plutonium_
// */
//@Info(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Library",
//    description = "Provides access to database.",
//    howto = "Available actions:\n" +
//            "    Item left click : Selects item\n" +
//            "    Item right click : Opens context menu\n" +
//            "    Item double click : Plays item\n" +
////            "    Item drag : \n" +
//            "    Press ENTER : Plays item\n" +
//            "    Press ESC : Clear selection & filter\n" +
////            "    Type : Searches for item - applies filter\n" +
//            "    Scroll : Scroll table vertically\n" +
//            "    Scroll + SHIFT : Scroll table horizontally\n" +
//            "    Drag column : Changes column order\n" +
//            "    Click column : Changes sort order - ascending,\n" +
//            "                   descending, none\n" +
//            "    Click column + SHIFT : Sorts by multiple columns\n",
//    notes = "",
//    version = "0.6",
//    year = "2014",
//    group = LIBRARY
//)
//public class LibraryViewController extends FXMLController {
//    
//    private @FXML AnchorPane root;
//    private @FXML VBox content;
//    private final FilteredTable<MetadataGroup,MetadataGroup.Field> table = new FilteredTable<>(VALUE);
//    private Subscription dbMonitor;
//    private final List<String> ALL_COLUMNS = getAll_columns();
//    
//    // configurables
//    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
//    public final Accessor<NodeOrientation> table_orient = new Accessor<>(INHERIT, table::setNodeOrientation);
//    @IsConfig(name = "Zeropad numbers", info = "Adds 0 to uphold number length consistency.")
//    public final Accessor<Boolean> zeropad = new Accessor<>(true, table::setZeropadIndex);
//    @IsConfig(name = "Search show original index", info = "Show index of the table items as in unfiltered state when filter applied.")
//    public final Accessor<Boolean> orig_index = new Accessor<>(true, table::setShowOriginalIndex);
//    @IsConfig(name = "Show table header", info = "Show table header with columns.")
//    public final Accessor<Boolean> show_header = new Accessor<>(true, table::setHeaderVisible);
//    @IsConfig(name = "Show table menu button", info = "Show table menu button for controlling columns.")
//    public final Accessor<Boolean> show_menu_button = new Accessor<>(true, table::setTableMenuButtonVisible);
//    @IsConfig(editable = false)
//    private TableColumnInfo columnInfo = new TableColumnInfo(ALL_COLUMNS);
//    @IsConfig(name = "Library level", info = "")
//    public final Accessor<Integer> lvl = new Accessor<>(1, v -> {
//        if(dbMonitor!=null) dbMonitor.unsubscribe();
//        // listen for database changes to refresh library
//        dbMonitor = DB.views.subscribe(v, (i,list) -> {
//            setItems(list);
//        });
//        // initialize
//        setItems(DB.views.getValue(v));
//    });
//    @IsConfig(name = "Field")
//    public final AccessorEnum<Metadata.Field> fieldFilter = new AccessorEnum<Metadata.Field>(CATEGORY, v -> {
//        // repopulate
//        setItems(DB.views.getValue(lvl.getValue()));
//        find(table.getColumns(), c -> columnInfo.nameKeyMapper.apply(c.getText()).equals(VALUE.toStringEnum()))
//                .ifPresent(t -> t.setText(v.toStringEnum()));
//        
//        table.getSearchBox().setPrefTypeSupplier(() -> Tuples.t(VALUE.toString(v), VALUE.getType(v), VALUE));
//        table.getSearchBox().setData(list(MetadataGroup.Field.values(),
//                                          MetadataGroup.Field::isTypeStringRepresentable,
//                                          mgf->Tuples.t(mgf.toString(v),mgf.getType(v),mgf)));
//        },
//        ()->list(Metadata.Field.values(), Field::isTypeStringRepresentable)
//    );
//    
//
//    @Override
//    public void init() {
//        content.getChildren().addAll(table.getRoot());
//        VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
//        
//        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
//        table.getSelectionModel().setSelectionMode(MULTIPLE);
//        
//        // add index column
//        TableColumn indexColumn = createIndexColumn("#");
//        table.getColumns().add(indexColumn);
//        
//        // context menu
//        table.setOnMouseClicked( e -> {
//            if (e.getY()<table.getFixedCellSize()) return;
//            if(e.getButton()==PRIMARY) {
//                if(e.getClickCount()==2)
//                    play(table.getSelectedItemsCopy());
//            } else
//            if(e.getButton()==SECONDARY)
//                contxt_menu.show(table, e);
//        });
//        
//        // key actions
//        table.setOnKeyReleased( e -> {
//            if (e.getCode() == ENTER)       // play first of the selected
//                play(table.getSelectedItems());
//            else if (e.getCode() == ESCAPE) // deselect
//                table.getSelectionModel().clearSelection();
//            else if (e.getCode() == K)      // layout columns
//                resizeMainColumn();
//        });
//        
//        // send selection changed events, do not use InvalidationListener
//        table.getSelectionModel().getSelectedItems().addListener(
//                (Observable o) -> forwardItems(DB.views.getValue(lvl.getValue())));
//        
//        // prevent scrol event to propagate up
//        root.setOnScroll(Event::consume);
//        
//        // prevent overly eager selection change
//        table.addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);
//        table.addEventFilter(MOUSE_RELEASED, consumeOnSecondaryButton);
//    }
//
//    @Override
//    public void refresh() {
//        
//        table_orient.applyValue();
//        zeropad.applyValue();
//        orig_index.applyValue();
//        show_header.applyValue();
//        show_menu_button.applyValue();
//        fieldFilter.applyValue();
//        // apply last as it loads data
//        lvl.applyValue();
//        
//        // set up columns - run only once
//        if(init) return;
//        columnInfo.nameKeyMapper = name -> {
//            if(name.equals("#")) return name;
//            if(Stream.of(MetadataGroup.Field.values()).anyMatch(m->m.toStringEnum().equals(name)))
//                return name;
//            else return VALUE.toStringEnum();
//        };
//        List<TableColumn<MetadataGroup,?>> tmp = new ArrayList();
//        columnInfo.columns.stream().sorted() // sort by index
//                          .forEach(c->{
//            // get or build column
//            TableColumn tc = null;
////            for(TableColumn t : table.getColumns())
////                if(c.name.equals(t.getText())) {
////                    break;
////                }
////            if(tc==null) 
//                tc = columnFactory.call(c.name);
//            // set width
//            // bug, last column sets twice the width, so divide it
//            double w = c.position==tmp.size()-1 ? c.width/2 : c.width;
//            tc.setPrefWidth(w);
//            // set visibility
//            tc.setVisible(c.visible);
//            // set position (automatically, because we sorted the input)
//            tmp.add(tc);
//        });
//        // restore
//        table.getColumns().setAll(tmp);
//        init=true;
//    }
//    
//    private boolean init = false;
//    private final Callback<String,TableColumn<MetadataGroup,?>> columnFactory = name -> {
//        if("#".equals(name))
//            return table.buildIndexColumn();
//        else {
//            MetadataGroup.Field f = MetadataGroup.Field.valueOfEnumString(name);
//            Metadata.Field v = fieldFilter.getValue();
//            TableColumn<MetadataGroup,?> c = new TableColumn(f.toString(v));
//            c.setCellValueFactory( cf -> {
//                if(cf.getValue()==null) return null;
//                return new ReadOnlyObjectWrapper(cf.getValue().getField(f));
//            });
//            String no_val = f==VALUE ? "<none>" : null;
//            c.setCellFactory(DEFAULT_ALIGNED_CELL_FACTORY(f.getType(v), no_val));
//            return c;
//        }
//    };
//    
//    private List<String> getAll_columns() {
//        List<String> l = new ArrayList();
//                     l.add("#");
//        l.addAll(listM(MetadataGroup.Field.values(), m->m.toStringEnum()));
//        return l;
//    }
//    
//    public void resizeMainColumn() {
//        find(table.getColumns(),
//             c->c.getText().equals(fieldFilter.getValue().toStringEnum()))
//            .ifPresent(c->{
//                double w = table.getColumns().stream().mapToDouble(TableColumn::getWidth).sum();
//                double itemsHeight = (table.getItems().size()+1)*table.getFixedCellSize();
//                double scrollbar = itemsHeight < table.getHeight() ? 0 : 15;
//                c.setPrefWidth(table.getWidth()-(scrollbar+w-c.getWidth()));
//            });
//    }
//
//    @Override
//    public void close() {
//        // stop listening for db changes
//        dbMonitor.unsubscribe();
//    }
//
//    @Override
//    public List getFields() {
//        // serialize column state as config
//        columnInfo.update(table);
//        return super.getFields(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Config getField(String name) {
//        if("columnInfo".equals(name)) columnInfo.update(table);
//        return super.getField(name); //To change body of generated methods, choose Tools | Templates.
//    }
//    
//    
///******************************** PRIVATE API **********************************/
//    
//    private final Histogram<Object, Metadata, Tuple4<Long,Set<String>,Double,Long>> h = new Histogram();
//    
//    /** populates metadata groups to table from metadata list */
//    private void setItems(List<Metadata> list) {
//        Field f = fieldFilter.getValue();
//        // make histogram
//        h.keyMapper = metadata -> metadata.getField(f);
//        h.histogramFactory = () -> new Tuple4(0l,new HashSet(),0d,0l);
//        h.elementAccumulator = (hist,metadata) -> {
//            hist.a++;
//            hist.b.add(metadata.getAlbum());
//            hist.c += metadata.getLengthInMs();
//            hist.d += metadata.getFilesizeInB();
//        };
//        h.clear();
//        h.accumulate(list);
//        // read histogram
//        table.setItemsRaw(h.toList((value,s)->new MetadataGroup(f, value, s.a, s.b.size(), s.c, s.d)));
//        // pass down the chain
//        forwardItems(list);
//    }
//    
//    /** Sends event to next level. */
//    private void forwardItems(List<Metadata> list) {
//        List<Metadata> forwardList;
//        if(table.getSelectionModel().isEmpty()) {
//            // no selection -> fetch everything, this is simplified behavior as
//            // filter might apply, in such case we should filter the items too
//            // i fear of bad performance, say predicate compound of 2000 predicates or even more
//            forwardList = list;
//        } else {
//            Predicate<Metadata> p = table.getSelectedItems().stream()
//                    .map(MetadataGroup::toMetadataPredicate)
//                    .reduce(Predicate::or)
//                    .orElse(isFALSE);
//            forwardList = list.stream().filter(p).collect(toList());
//        }
//        DB.views.push(lvl.getValue()+1, forwardList);
//    }
//    
///******************************** CONTEXT MENU ********************************/
//    
//    private static final TableContextMenuInstance<MetadataGroup> contxt_menu = new TableContextMenuInstance<>(
//        () -> {
//            ContentContextMenu<List<MetadataGroup>> m = new ContentContextMenu();
//            
//            m.getItems().addAll(
//                createmenuItem("Play items", e -> play(m.getValue())),
//                createmenuItem("Enqueue items", e -> PlaylistManager.addItems(dbFetch(m.getValue()))),
//                createmenuItem("Update from file", e -> DB.updateItemsFromFile(dbFetch(m.getValue()))),
//                createmenuItem("Remove from library", e -> DB.removeItems(dbFetch(m.getValue()))),
//                createmenuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(dbFetch(m.getValue())))));
////                createmenuItem("Edit the item/s in tag editor", e -> m.getV);
//            return m;
//        },
//        (menu,table) -> menu.setValue(ImprovedTable.class.cast(table).getSelectedItemsCopy())
//    );
//    
//    private static List<Metadata> dbFetch(List<MetadataGroup> filters) {
//        return DB.getAllItemsWhere(filters.get(0).getField(), filters.get(0).getValue());
//    }
//    private static void play(List<MetadataGroup> filters) {
//        if(filters.isEmpty()) return;
//        List<PlaylistItem> to_play = new ArrayList();
//        dbFetch(filters).stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
//        PlaylistManager.playPlaylist(new Playlist(to_play));
//    }
//}