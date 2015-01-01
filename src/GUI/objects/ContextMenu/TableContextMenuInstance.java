/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.ContextMenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author Plutonium_
 */
public class TableContextMenuInstance<E> extends TableContextMenuRInstance<E,TableView<E>> {
    
    public TableContextMenuInstance(Supplier<ContentContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TableContextMenuInstance(Supplier<ContentContextMenu<List<E>>> builder, BiConsumer<ContentContextMenu<List<E>>, TableView<E>> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code get(table).show(table, e)} . But called only if the
     * table selection is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(TableView<E> table, MouseEvent e) {
        show(table, table, e);
    }
    
}
