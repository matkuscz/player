
package Layout.Areas;

import GUI.DragUtil;
import GUI.GUI;
import GUI.WidgetTransfer;
import GUI.objects.Pickers.WidgetPicker;
import Layout.AltState;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.PolyContainer;
import Layout.Widgets.WidgetInfo;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import utilities.Animation.Interpolators.CircularInterpolator;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;

/**
 * @author uranium
 * 
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@WidgetInfo
public final class Layouter implements AltState {
    
    private static final Duration ANIM_DUR = Duration.millis(300);
    private int index;              // hack (see to do API section, layouts)
    
    private @FXML BorderPane controls;
    private @FXML AnchorPane root = new AnchorPane();
    private @FXML AnchorPane content;
    private final Container container;
    private final FadeTransition anim;
    private final ScaleTransition animS;
    
    public Layouter(Container con, int index) {
        this.index = index;
        this.container = con;
        
        FXMLLoader fxmlLoader = new FXMLLoader(Layouter.class.getResource("Layouter.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        Interpolator i = new CircularInterpolator(EASE_OUT);
        anim = new FadeTransition(ANIM_DUR, content);
        // anim.setInterpolator(i); // use the LINEAR by defaul instead
        animS = new ScaleTransition(ANIM_DUR, content);
        animS.setInterpolator(i);
            // initialize state for animations
        content.setOpacity(0);
        
        // initialize mode
        setWeakMode(false); // this needs to be called in constructor
        
        // prevent action & allow passing mouse events when not fully visible
        content.mouseTransparentProperty().bind(content.opacityProperty().isNotEqualTo(1));
        
        // do not support drag from - no content
        // but accept drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF)) {
                WidgetTransfer wt = DragUtil.getWidgetTransfer(db);
                int i1 = index;
                container.swapChildren(wt.getContainer(),i1,wt.childIndex());
                e.consume();
            }
        });
    }

/****************************  functionality  *********************************/
    
    @Override
    public void show() {
        showControls(true);
    }

    @Override
    public void hide() {
        // prevent leaving layout mode when layout mode active
        if(GUI.isLayoutMode())return;
        showControls(false);
    }
    
    private void showControls(boolean val) {
        anim.stop();
        animS.stop();
        if (val) {
            anim.setToValue(1);
            animS.setToX(1);
            animS.setToY(1);
        } else {
            anim.setToValue(0);
            animS.setToX(0);
            animS.setToY(0);
            if(content.getChildren().size()>1 && !end) {
                animS.setOnFinished( ae -> {
                    controls.setVisible(true);
                    content.getChildren().retainAll(controls);
                    animS.setOnFinished(null);
                });
            }
        }
        anim.play();
        animS.play();
    }
    
    private boolean weakMode = false;
    
    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     * Default false.
     * @param val 
     */
    public void setWeakMode(boolean val) {
        weakMode = val;
        
        // always hide on mouse exit, but make sure it is initialized
        if (root.getOnMouseExited()==null)
            root.setOnMouseExited(controlsHider);
        // swap handlers
        if(val) {
            root.setOnMouseClicked(null);
            root.setOnMouseEntered(controlsShower);
        } else {
            root.setOnMouseClicked(controlsShower);
            root.setOnMouseEntered(null);
        }
    }
    
    public void toggleWeakMode() {
        weakMode = !weakMode;
    }
    public boolean isWeakMode() {
        return weakMode;
    }
    
    private final EventHandler<MouseEvent> controlsShower =  e -> {
        // avoid right click activation
        if(e.getEventType().equals(MOUSE_CLICKED) && e.getButton()!=PRIMARY) return;
        // rely on the public show() implementation, not internal one
        show();
        e.consume();
    };
    private final EventHandler<MouseEvent> controlsHider =  e -> {
        // rely on the public show() implementation, not internal one
        hide();
        e.consume();
    };

    // quick fix to prevent overwriting onFinished animation handler
    boolean end = false;
    @FXML
    private void showWidgetArea(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        WidgetPicker w = new WidgetPicker();
        w.setOnSelect(f -> {
            end = true;
            animS.setOnFinished( a -> {
                // actually not needed since layouter is removed when widget is
                // loaded in the container in its place
//                controls.setVisible(true);
//                content.getChildren().retainAll(controls);
//                animS.setOnFinished(null);
                // this is the crucial part
                container.addChild(index, f.create());
            });
            showControls(false);
        });
        
        animS.setOnFinished( ae -> {
            Node n = w.getNode();
            content.getChildren().add(n);
            AnchorPane.setBottomAnchor(n, 0d);
            AnchorPane.setTopAnchor(n, 0d);
            AnchorPane.setRightAnchor(n, 0d);
            AnchorPane.setLeftAnchor(n, 0d);
            controls.setVisible(false);
            showControls(true);
            animS.setOnFinished(null);
        });
        showControls(false);
        
        e.consume();
    }
    @FXML
    private void showSplitV(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        container.addChild(index, new BiContainerPure(HORIZONTAL));
        
        e.consume();
    }
    @FXML
    private void showSplitH(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        container.addChild(index, new BiContainerPure(VERTICAL));
        
        e.consume();
    }
    @FXML
    private void showTabs(MouseEvent e) {
        if(e.getButton()!=PRIMARY) return;
        
        container.addChild(index, new PolyContainer());
        
        e.consume();
    }
    
    
    public Parent getRoot() {
        return root;
    }
}

