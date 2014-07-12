/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.LayoutAggregators;

import Layout.Layout;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

/**
 *
 * @author Plutonium_
 */
public class EmptyLayoutAggregator implements LayoutAggregator {

    /** {@inheritDoc} */
    @Override
    public List<Layout> getLayouts() {
        return EMPTY_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public Parent getRoot() {
        return new Region();
    }

    /** {@inheritDoc} */
    @Override
    public long getCapacity() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public Layout getActive() {
        return null;
    }
    
}
