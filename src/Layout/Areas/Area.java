/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import GUI.ContextManager;
import GUI.Window;
import Layout.AltState;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

/**
 * Graphical part of the container within layout.
 * The container - area is 1:1 non null relationship. Container makes up for the
 * abstract side, this class represents the graphical side.
 * <p>
 * The lifecycle of the graphics entirely depends on the lifecycle of the
 * container. Instances of this class can not live outside of container's
 * life cycle. Note that the opposite doesnt necessarily hold true.
 * <p>
 * @author uranium
 */
public abstract class Area<T extends Container> implements AltState {
    
    /**
     * Container this are is associated with. The relationship can not be changed.
     * Never null.
     */
    public final T container;
    /** The root Pane of this Area. Never null. */
    public final AnchorPane root = new AnchorPane();
    /** Activates controls */
    @FXML public Region activator;
    AreaControls controls;
    
    /**
     * 
     * @param _container must not be null
     */
    public Area(T _container) {
        // init final 1:1 container-area relationship
        Objects.requireNonNull(_container);
        container = _container;
        
        // init properties
        container.properties.initProperty(Double.class, "padding", 0d);
        container.properties.initProperty(Boolean.class, "locked", false);
        
        // init behavior
        root.setOnScroll(e -> {
            if(controlsOn) {
                if(e.getDeltaY()<0) collapse();
                else if(e.getDeltaY()>0) expand();
                e.consume();
            }
        });
        root.setOnMouseClicked(e->{
            if(controlsOn)
                if(e.getButton()==MouseButton.MIDDLE) {
                    setPadding(0);
                    e.consume();
                }
        });
    }
    
    /** @return all oomponents wrapped in the area. By default returns all
     * components of the container associated with the area.*/
    public List<Component> getAllComponents() {
        return new ArrayList<>(container.getChildren().values());
    }
    /** @return all active coponents - components that are being actively 
     * displayed. */
    abstract public  List<Component> getActiveComponents();
    /** @return the primary active component. */
    abstract public Component getActiveComponent();
    
    /**
     * Refresh area. Refreshes the content - wrapped components by calling their
     * refresh() method. Some components might not support this behavior and
     * will do nothing.
     * <p>
     * Implementation decides which components need and will get refreshed.
     * <p>
     * Default implementation refreshes all active widgets (ignores containers).
     */
    public void refresh() {
        for(Component c: getActiveComponents()) {
            if(c instanceof Widget) 
                ((Widget)c).getController().refresh();
        }
    }
    /**
     * Adds component to the area.
     * <p>
     * Implementation decides how exactly. Simple
     * implementation storing single component would remove the old component
     * from layout map and add new one to the parent container.
     * @param c 
     */
    abstract public void add(Component c);
    
    /**
     * Detaches the content into standalone content. Opens new window.
     * <p>
     * Implementation decides specifics of the operation. {@link PolyArea} could
     * detach only some components or itself or only active component.
     * <p>
     * Default implementation detaches the first active component. Does nothing
     * if no active component available.
     */
    public void detach() {
        // get first active component
        Component c = getActiveComponent();
        // return if empty
        if (c==null) return;
        
        // detach into new window
        Window w = ContextManager.openFloatingWindow(Widget.EMPTY());
               w.setSize(root.getWidth(), root.getHeight());
        container.swapChildren(c, w.getLayout(), w.getLayout().getChild());
    }
    
    public void close() {
        container.close();
    }
    
/******************************************************************************/
    
    /** Returns the content. */
    abstract public AnchorPane getContent();
    
/******************************************************************************/
    
    public final void expand() {
        changePadding(-1);
    }
    public final void collapse() {
        changePadding(+1);
    }
    public final void changePadding(double by) {
        Insets pad = root.getPadding();
        double to = pad.getTop()+by;
        if(to<0) to = 0;
        else if(to>root.getWidth()/2) to = root.getWidth()/2;
        else if(to>root.getHeight()/2) to = root.getHeight()/2;
        
        setPadding(to);
    }
    public final void setPadding(double to) {
        // update properties if changed
        if(root.getPadding().getTop()!=to)
            container.properties.set("padding", to);
        root.setPadding(new Insets(to));
    }
    
/******************************* layout mode **********************************/
    
    protected boolean controlsOn = false;
    
    @FXML
    @Override
    public void show() {
        controls.show();
        controlsOn = true;
    }
    @FXML
    @Override
    public void hide() {
        controls.hide();
        controlsOn = false;
    }
    @FXML
    public void setLocked(boolean val) {
        container.setLocked(val);
    }
    public boolean isLocked() {
        return container.isLocked();
    }
    public boolean isUnderLock() {
        return container.isUnderLock();
    }
    @FXML
    public void toggleLocked() {
        container.toggleLock();
    }    
    
    public void enableContent() {
        getContent().setMouseTransparent(false);
    }
    public void disableContent() {
        getContent().setMouseTransparent(true);
    }
}