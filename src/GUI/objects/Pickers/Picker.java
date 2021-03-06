
package gui.objects.Pickers;

import static java.lang.Math.*;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.animation.Transition;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import util.Animation.Anim;
import static util.Animation.Anim.Interpolators.isAroundMin1;
import static util.Animation.Anim.par;
import static util.Animation.Anim.seq;
import util.functional.Util;
import static util.functional.Util.forEachI;
import static util.functional.Util.forEachIStream;
import util.functional.functor.FunctionC;
import util.parsing.ToStringConverter;

/**
 * Generic item picker.
 * <p>
 * Node displaying elements as grid.
 * Elements are converted to their text representation according to provided
 * mapper. Element should override toString() method if no mapper is provided. 
 * <p>
 * Elements will be sorted lexicographically.
 * <p>
 * Elements will be represented graphically depending on the cell factory.
 * <p>
 * @author Plutonium_
 */
public class Picker<E> {
    
    /** Style class for cell. */
    public static final String STYLE_CLASS = "item-picker";
    public static final List<String> CELL_STYLE_CLASS = asList("block","item-picker-element");
    /** Default on select action. Does nothing. */
    public static final Consumer DEF_onSelect = item -> {};
    /** Default on cancel action. Does nothing. */
    public static final Runnable DEF_onCancel = () -> {};
    /** Default Text factory. Uses item's toString() method. */
    public static final ToStringConverter DEF_textCoverter = Object::toString;
    /** Default Item supplier. Returns empty stream. */
    public static final Supplier<Stream> DEF_itemSupply = Stream::empty;
    
    private final AnchorPane tiles = new AnchorPane();
    public final ScrollPane root = new ScrollPane(tiles);
    
    /**
     * Procedure executed when item is selected passing the item as parameter.
     * Default implementation does nothing. Must not be null;
     */
    public Consumer<E> onSelect = DEF_onSelect;
    /**
     * Procedure executed when no item is selected. Invoked when user cancels
     * the picking by right click.
     * Default implementation does nothing. Must not be null;
     * <p>
     * For example one might want to close this picker when no item is selected.
     */
    public Runnable onCancel = DEF_onCancel;
    /**
     * Text factory.
     * Creates string representation of the item.
     * Default implementation uses item's toString() method.
     * Must not be null.
     */
    public ToStringConverter<E> textCoverter = DEF_textCoverter;
    /**
     * Item supplier. Fetches the items as a stream. 
     * Default implementation returns empty stream. Must not be null;
     */
    public Supplier<Stream<E>> itemSupply = (Supplier)DEF_itemSupply;
    /**
     * Cell factory.
     * Creates graphic representation of the item.
     * Also might define minimum and maximum item size.
     * Must not be null;
     */
    public FunctionC<E,Region> cellFactory = item -> {
        String text = textCoverter.toS(item);
        Label l = new Label(text);
        StackPane b = new StackPane(l);
        b.getStyleClass().setAll(CELL_STYLE_CLASS);
        StackPane a = new StackPane(b);
        a.setMinSize(90, 30);
        return a;
    };
    
    public Picker() {
        // auto-layout
        root.widthProperty().addListener((o,ov,nv) -> layout(nv.doubleValue(), root.getHeight()));
        root.heightProperty().addListener((o,ov,nv) -> layout(root.getWidth(), nv.doubleValue()));
        
        root.setPannable(false);  // forbid mouse panning
        root.setHbarPolicy(NEVER);
        root.setPrefSize(-1,-1);  // leave resizable
        root.setFitToWidth(true); // make content resize with scroll pane        
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        root.addEventFilter(MOUSE_PRESSED, Event::consume);
        root.addEventFilter(MOUSE_DRAGGED, Event::consume);
        root.setOnMouseClicked(e->{
            if(e.getButton()==SECONDARY) {
                onCancel.run();
//                e.consume(); // causes problems for layouter, off for now
            }
        });
        root.getStyleClass().add(STYLE_CLASS);
    }
    
    public void buildContent() {
        tiles.getChildren().clear(); 
        // get items
        itemSupply.get()
            // & sort
            .sorted(Util.byNC(textCoverter::toS))
            // & create cells
            .forEach( item -> {
                Node cell = cellFactory.apply(item);
                     cell.setOnMouseClicked( e -> {
                         if(e.getButton()==PRIMARY) {
                            onSelect.accept(item);
                            e.consume();
                         }
                     });
                tiles.getChildren().add(cell);
            });
        
        getCells().forEach( c -> c.setBorder(new Border(new BorderStroke(new Color(0,0,0,0.2), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2)))));
//        getCells().forEach( c -> c.setBorder(new Border(new BorderStroke(c.getBackground().getFills().get(0).getFill(), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, null))));
        Transition t = par(
          forEachIStream(getCells(), (i,n) ->
              seq(
                new Anim(n.getChildrenUnmodifiable().get(0)::setOpacity).dur(500+random()*1000).intpl(0),
                new Anim(n.getChildrenUnmodifiable().get(0)::setOpacity).dur(500).intpl(isAroundMin1(0.04, 0.1,0.2,0.3))
              )
          )
        );
        t.setOnFinished(e -> getCells().forEach( c -> c.setBorder(null)));
        t.play();
        
//        Interpolator in = new BounceInterpolator();
////        getCells().forEach(n -> setScaleXY(n,0));
//        getCells().forEach(n -> n.setOpacity(0));
//        Interpolator in = new BounceInterpolator();
//        par(millis(500),
//            forEachIndexedStream(getCells(), (i,n) -> 
//                par(millis(abs(getCells().size()/2-i)*100),
//                        new Anim(millis(300), in, at -> n.setOpacity(isAroundMin(at, 0.02, 0.5+0.05, 0.5+0,15, 0.5+0.2, 0.5+0.35) ? 0 : 1))
////                    seq(
////                        new Anim(millis(450), in, at -> setScaleXY(n, at, 0.2)),
////                        new Anim(millis(300), in, at -> setScaleXY(n, 1, 0.2+0.8*at)),
////                    )
////                    new Anim(millis(300), in, at -> n.setOpacity(isAround(0.04, 0.5, 0,65, 0.8, 0.95).test(at) ? 0 : 1))
//                )
//           )
//        ).play();
        
    }
    
    public Node getNode() {
        buildContent();
        return root;
    }
    
    public List<Region> getCells() {
        return new ArrayList(tiles.getChildren());
    }
    
    private void layout(double width, double height) {
        int gap = 5;
        int elements = tiles.getChildren().size();
        double min_cell_w = max(1,getCells().get(0).getMinWidth());
        double min_cell_h = max(1,getCells().get(0).getMinHeight());
//        if(elements==0) return;
        
        int c = width>height ? (int) ceil(sqrt(elements)) : (int) floor(sqrt(elements));
            c = width<c*min_cell_w ? (int)floor(width/min_cell_w) : c;
        final int columns = max(1,c);
        
        int rows = (int) ceil(elements/(double)columns);
        
        double sumgapy = (rows-1) * gap;
        final double cell_height = height<rows*min_cell_h ? min_cell_h : (height-sumgapy)/rows-1/(double)rows;
        
        double W = rows*(cell_height+gap)-gap>height ? width-15 : width; // take care of scrollbar
        double sumgapx = (columns-1) * gap;  // n elements have n-1 gaps
        final double cell_width = (W-sumgapx)/columns;

        forEachI(getCells(), (i,n) -> {
            double x = i%columns * (cell_width+gap);
            double y = i/columns * (cell_height+gap);
            n.setLayoutX(x);
            n.setLayoutY(y);
            n.setPrefWidth(cell_width);
            n.setPrefHeight(cell_height);
        });
    }
}