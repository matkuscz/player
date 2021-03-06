
package Layout;

import Layout.Areas.ContainerNode;
import Layout.Areas.Layouter;
import Layout.Areas.WidgetArea;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import static util.Util.setAnchors;


/**
 * 
 * Implementation of {@link Container Container} storing one child component. It
 * is expected the child will be non-Container component as putting any container
 * within this would turn this into unnecessary intermediary.
 * <p>
 * @author uranium
 */
public class UniContainer extends Container {
    
    Component child;
    @XStreamOmitField
    ContainerNode graphics;
    
    public UniContainer() {
    }
    public UniContainer(AnchorPane root_pane) {
        root = root_pane;
    }

    @Override
    public ContainerNode getGraphics() {
        return graphics;
    }
    
    @Override
    public Node load() {
        Node out;
        
        if (child instanceof Container) {
            removeGraphicsFromSceneGraph();
            graphics = null;
            out = Container.class.cast(child).load(root);
        } else 
        if (child instanceof Widget) {
            if (!(graphics instanceof WidgetArea)) {
                removeGraphicsFromSceneGraph();
                graphics = new WidgetArea(this, 1);
            }
            WidgetArea.class.cast(graphics).loadWidget((Widget)child);
            out = graphics.getRoot();
        } else {
            graphics = new Layouter(this,1);
            out = graphics.getRoot();
        }
        
        root.getChildren().setAll(out);
        setAnchors(out,0);
        
        return out;
    }
    
    
    @Override
    public Map<Integer, Component> getChildren() {
        // override with more effective implementation
        return Collections.singletonMap(1, child);
    }
    
    /**
     * Convenience method. Equal to getChildreg.get(1). It is
     * recommended to use this method if standard Map format is not necessary.
     * @return child or null if none present.
     */
    public Component getChild() {
        return child;
    }
    
    /** 
     * {@inheritDoc} 
     * This implementation considers all index values valid, except for null,
     * which will be ignored.
     */
    @Override
    public void addChild(Integer index, Component c) {
        if(index==null) return;
        
        if (c instanceof Container)
            Container.class.cast(c).parent = this;
        
        child = c;
        load();
        initialize();
    }
    
    /**
     * Convenience method. Equal to addChild(1, w);
     * @param w 
     */
    public void setChild(Component w) {
        addChild(1, w);
    }
    
    @Override
    public Integer indexOf(Component c) {
        if (Objects.equals(c, child)) return 1;
        else return null;
    }    

    @Override
    public Integer getEmptySpot() {
        return child==null ? 1 : null;
    }
}