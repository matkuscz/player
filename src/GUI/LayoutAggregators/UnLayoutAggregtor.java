/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.LayoutAggregators;

import Layout.Layout;
import java.util.Collections;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Plutonium_
 */
public class UnLayoutAggregtor implements LayoutAggregator {
    private Layout l;
    private AnchorPane r;
    
    public UnLayoutAggregtor(Layout layout) {
        r = new AnchorPane();
        l = layout;
        l.setRoot(r);
    }

    /** {@inheritDoc} */
    @Override
    public List<Layout> getLayouts() {
        return Collections.singletonList(l);
    }

    /** {@inheritDoc} */
    public Layout getLayout() {
        return l;
    }

    /** {@inheritDoc} */
    @Override
    public Parent getRoot() {
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public long getCapacity() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public Layout getActive() {
        return l;
    }
    
}