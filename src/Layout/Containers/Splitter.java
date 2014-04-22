
package Layout.Containers;

import Configuration.PropertyMap;
import GUI.GUI;
import GUI.objects.SimplePositionable;
import Layout.AltState;
import Layout.BiContainer;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.Widget;
import PseudoObjects.TODO;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/**
 * @author uranium
 *
 */
@TODO("resizing when collapsed slow response & boilerplate code")
public final class Splitter implements AltState {
    private static final FXMLLoader fxmlLoader = new FXMLLoader(Splitter.class.getResource("Splitter.fxml"));
    
    AnchorPane root = new AnchorPane();
    @FXML AnchorPane child1;
    @FXML AnchorPane child2;
    @FXML SplitPane splitPane;
    @FXML AnchorPane controlsPane;
    
    SimplePositionable controls;
    BiContainer container;
    
    //tmp variables
    private final PropertyMap prop;
    private final FadeTransition fadeIn;
    private final FadeTransition fadeOut;

    public Splitter(BiContainer con) {
        container = con;
        prop = con.properties;
        
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) { 
            throw new RuntimeException(e);
        }
        
        // initialize properties
        prop.initProperty(Double.class, "pos", 0.5d);
        prop.initProperty(Orientation.class, "orient", Orientation.VERTICAL);
        prop.initProperty(Boolean.class, "lock", false); //true=not locked size
        prop.initProperty(Double.class, "collap", 0d);
       
        // set properties  // some must be inside Platform.runLater() <= java bug
        splitPane.setDividerPosition(0, prop.getD("pos"));
        splitPane.setOrientation(prop.getOriet("orient"));
        SplitPane.setResizableWithParent(child2, prop.getB("lock"));
        Platform.runLater(() -> {
            splitPane.setDividerPosition(0, prop.getD("pos"));
            splitPane.setOrientation(prop.getOriet("orient"));
            SplitPane.setResizableWithParent(child2, prop.getB("lock"));
            if (getCollapsed()<0)
                splitPane.setDividerPosition(0,0);
            if (getCollapsed()>0)
                splitPane.setDividerPosition(0,1);
        });
        
        // controls behavior
        controls = new SimplePositionable(controlsPane, root);
        splitPane.getDividers().get(0).positionProperty().addListener( o -> {
            positionControls();
            prop.set("pos", splitPane.getDividers().get(0).getPosition());
        });
        root.heightProperty().addListener( o -> {
            positionControls();
        });
        positionControls();
        
        splitPane.heightProperty().addListener( o -> {
            Platform.runLater(() -> {
                if (getCollapsed()<0)
                    splitPane.setDividerPositions(0);
                if (getCollapsed()>0)
                    splitPane.setDividerPositions(1);
            });
        });
        splitPane.widthProperty().addListener( o -> {
            Platform.runLater(() -> {
                if (getCollapsed()<0)
                    splitPane.setDividerPositions(0);
                if (getCollapsed()>0)
                    splitPane.setDividerPositions(1);
            });
        });
        
        // build animations
        fadeIn = new FadeTransition(TIME, controlsPane);
        fadeIn.setToValue(1);
        fadeOut = new FadeTransition(TIME, controlsPane);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished( e -> {
            controls.getPane().setMouseTransparent(true);
        });

        // activate animation if mouse close to divider
        final double limit = 5; // distance for activation of the animation
        splitPane.addEventFilter(MouseEvent.MOUSE_MOVED, t -> {
            if (splitPane.getOrientation() == Orientation.HORIZONTAL) {
                double X = splitPane.getDividerPositions()[0] * root.widthProperty().get();
                if (Math.abs(t.getX() - X) < limit)
                    showControls();
                else
                if (Math.abs(t.getX() - X) > limit)
                    hideControls();
            } else {
                double Y = splitPane.getDividerPositions()[0] * root.heightProperty().get();
                if (Math.abs(t.getY() - Y) < limit)
                    showControls();
                else
                if (Math.abs(t.getY() - Y) > limit)
                    hideControls();
            }
        });
        
        // activate animation if mouse if leaving area
        splitPane.addEventFilter(MouseEvent.MOUSE_EXITED, t -> {
            if (!splitPane.contains(t.getX(), t.getY())) // the contains check is necessary to avoid mouse over button = splitPane pane mouse exit
                hideControls();
        });
        
        positionControls();
        hideControls();
    }

    public void setChild1(Component w) {
        if (w == null) return;
        // repopulate
        if (w instanceof Widget) {
            Node child = w.load();
            child1.getChildren().clear();
            child1.getChildren().add(child);
            // bind child to the area
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
        }
        if (w instanceof Container) {
            Node child = ((Container)w).load(child1);
        }
    }
    public void setChild2(Component w) {
        if (w == null) return;
        if (w instanceof Widget) {
            // repopulate
            Node child = w.load();
            child2.getChildren().clear();
            child2.getChildren().add(child);
            // bind child to the area
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
        }
        if (w instanceof Container) {
            Node child = ((Container)w).load(child2);
        }
    }

    public AnchorPane getChild1Pane() {
        return child1;
    }
    public AnchorPane getChild2Pane() {
        return child2;
    }
    public AnchorPane getPane() {
        return root;
    }
    private void positionControls() {
        if (splitPane.getOrientation() == Orientation.VERTICAL) {
            double X = splitPane.getWidth()/2;
            double Y = splitPane.getDividerPositions()[0] * root.heightProperty().get();
            controls.relocate(X-controls.getWidth()/2, Y-controls.getHeight()/2);
        } else {
            double X = splitPane.getDividerPositions()[0] * root.widthProperty().get();
            double Y = splitPane.getHeight()/2;
            controls.relocate(X-controls.getWidth()/2, Y-controls.getHeight()/2);
        }
    }
//    public void setOrientation(Orientation o) {
//        prop.set("orient", o);
//        splitPane.setOrientation(o);
//    }
    /**
     * Toggle orientation between vertical/horizontal.
     */
    @FXML
    public void toggleOrientation() {
        if (splitPane.getOrientation() == Orientation.HORIZONTAL) {
            prop.set("orient", Orientation.VERTICAL);
            splitPane.setOrientation(Orientation.VERTICAL);
        } else {
            prop.set("orient", Orientation.HORIZONTAL);
            splitPane.setOrientation(Orientation.HORIZONTAL);
        }
    }
    /**
     * Toggle fixed size on/off.
     */
    @FXML
    public void toggleLock() {
        if (SplitPane.isResizableWithParent(child2)) {
            prop.set("lock", false);
            SplitPane.setResizableWithParent(child2, false);
        } else {
            prop.set("lock", true);
            SplitPane.setResizableWithParent(child2, true);
        }  
    }
    /**
     * Switch positions of the children
     */
    @FXML
    public void switchChildren() {
        container.switchCildren();
    }
    /**
     * Collapse on/off to the left or top depending on the orientation.
     */
    public void toggleCollapsed1() {
        if (isCollapsed()) {
            splitPane.setDividerPosition(0, Math.abs(getCollapsed()));
            prop.set("collap", 0.0d);
        } else {
            prop.set("collap", -splitPane.getDividerPositions()[0]);
            splitPane.setDividerPosition(0, 0.0d);
        }  
    }
    /**
     * Collapse on/off to the right or bottom depending on the orientation.
     */
    public void toggleCollapsed2() {
        if (isCollapsed()) {
            splitPane.setDividerPosition(0, Math.abs(getCollapsed()));
            prop.set("collap", 0.0d);
        } else {
            prop.set("collap", splitPane.getDividerPositions()[0]);
            splitPane.setDividerPosition(0, 1.0d);
        }  
    }
    public boolean isCollapsed() {
        return getCollapsed() != 0;
    }
    public double getCollapsed() {
        return prop.getD("collap");
    }
    
    @FXML
    public void close() {
        container.close();
    }
    
    
    public void showControls() {
        if (!GUI.alt_state) return;
        fadeIn.play();
        controls.getPane().setMouseTransparent(false);
    }
    
    public void hideControls() {
        fadeOut.play();
    }
    
    @Override
    public void show() {
        showControls();
    }

    @Override
    public void hide() {
        hideControls();
    }
}