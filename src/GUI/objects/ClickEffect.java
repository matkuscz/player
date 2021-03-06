
package gui.objects;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import gui.objects.Window.stage.Window;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import static javafx.stage.StageStyle.TRANSPARENT;
import javafx.util.Duration;
import jdk.nashorn.internal.ir.annotations.Immutable;
import main.App;
import util.dev.TODO;

/**
 * 
 * @author uranium
 */
@Immutable
@IsConfigurable
@TODO(purpose = TODO.Purpose.UNIMPLEMENTED, note = "make mouse transparent")
public class ClickEffect {
    
    // configuration
    @IsConfig(name = "Show click effect", info = "Show effect on click.")
    public static boolean show_clickEffect = true;
    @IsConfig(name = "Show trail effect", info = "Show cursor trail effect.")
    public static boolean show_trailEffect = true;
    @IsConfig(name = "Click effect duration", info = "Duration of the click effect in milliseconds.")
    public static double DURATION = 350;
    @IsConfig(name = "Click effect min", info = "Starting scale value of cursor click effect animation.")
    public static double MIN_SCALE = 0.2;
    @IsConfig(name = "Click effect max", info = "Ending scale value of cursor click effect animation.")
    public static double MAX_SCALE = 0.7;
    @IsConfig(name="Click effect delay", info = "Delay of the click effect in milliseconds.")
    public static double DELAY = 0;
    @IsConfig(name="Trail effect intensity", info = "Intensity of the cursor trail effect. Currently logarithmic.", min = 1, max = 100)
    public static double effect_intensity = 50;
    @IsConfig(name="Blend Mode", info = "Blending mode for the effect.")
    public static BlendMode blend_mode = BlendMode.SRC_OVER;
    
    @AppliesConfig("show_clickEffect")
    private static void applyShowClickEffect() {
        if(show_clickEffect) Window.windows.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(MOUSE_PRESSED, clickHandler));
        else Window.windows.forEach(w -> w.getStage().getScene().getRoot().removeEventFilter(MOUSE_PRESSED, clickHandler));
    }
    
    @AppliesConfig("show_trailEffect")
    private static void applyShowTrailEffect() {
        if(show_trailEffect) Window.windows.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(MOUSE_MOVED, trailHandler));
        else Window.windows.forEach(w -> w.getStage().getScene().getRoot().removeEventFilter(MOUSE_MOVED, trailHandler));
    }
    
    @AppliesConfig( "blend_mode")
    @AppliesConfig( "DURATION")
    @AppliesConfig( "MIN_SCALE")
    @AppliesConfig( "MAX_SCALE")
    @AppliesConfig( "DELAY")
    private static void applyEffectAttributes() {
        if(App.isInitialized())
        pool.forEach(ClickEffect::apply);
    }
    
    
    // pooling
    private static final List<ClickEffect> pool = new ArrayList();
    private static int counter = 0;
    private static int getCounter() { return counter; }
    private static double getTrailIntensity() { return effect_intensity; }
    
    // creating
    public static ClickEffect create() {
        if (pool.isEmpty())
            return new ClickEffect();
        else {
            ClickEffect c = pool.get(0);
            pool.remove(0);
            return c;
        }
    }
    
    public static ClickEffect createStandalone() {
        return new ClickEffect();
    }
    
    /**
     * Run at specific coordinates. The graphics of the effect is centered - [0,0]
     * is at its center
     * system.
     * @param X
     * @param Y 
     */
    public static void run(double X, double Y) {
        create().play(X, Y);
    }
    
    // handlers to display the effect, set to window's root
    private static final EventHandler<MouseEvent> clickHandler = e ->
            run(e.getScreenX(), e.getScreenY());
    
    private static final EventHandler<MouseEvent> trailHandler = e -> {
            counter = counter==100 ? 1 : counter+1;
            if (counter%(101-(int)effect_intensity) == 0)
                run(e.getScreenX(), e.getScreenY());
        };
    
/******************************************************************************/
    
    // fields
    private final Circle root = new Circle();
    private final FadeTransition fade = new FadeTransition();
    private final ScaleTransition scale = new ScaleTransition();
    private final ParallelTransition anim = new ParallelTransition(root,fade,scale);
    
    private ClickEffect() {
        root.setRadius(15);
        root.setFill(null);
        root.setEffect(new GaussianBlur(5.5));
        root.setStroke(Color.AQUA);
        root.setStrokeWidth(4.5);
        
        root.setVisible(false);
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);
        root.setMouseTransparent(true);
        anim.setOnFinished( e ->pool.add(this));
        
        ((Pane) s.getScene().getRoot()).getChildren().add(root);
        
        apply();
    }
    
    private static final Stage s;
    static { 
        AnchorPane root = new AnchorPane();
                   root.setMouseTransparent(true);
                   root.setStyle("-fx-background-color: null;");
                   root.setPickOnBounds(false);
        s = new Stage(TRANSPARENT);
        s.initOwner(App.getWindowOwner().getStage());
        s.setScene(new Scene(root));
        s.setX(1);
        s.setY(1);
        s.setWidth(Screen.getPrimary().getBounds().getWidth()-2);
        s.setHeight(Screen.getPrimary().getBounds().getHeight()-2);
        s.setAlwaysOnTop(true);
        s.getScene().setFill(null);
//        s.show();
    }
    
    private double scaleB = 1;
    public ClickEffect setScale(double s) {
        scaleB = s;
        return this;
    }
    public void play(double X, double Y) {
        // center position on run
        root.setLayoutX(X);
        root.setLayoutY(Y);
        // run effect
        root.setVisible(true);
        anim.play();
    }
    
    public void apply() {
        root.setBlendMode(blend_mode);
        anim.setDelay(Duration.millis(DELAY));
        
        fade.setDuration(Duration.millis(DURATION));
        fade.setFromValue(0.6);
        fade.setToValue(0);
        
        scale.setDuration(Duration.millis(DURATION));
        scale.setFromX(scaleB*MIN_SCALE);
        scale.setFromY(scaleB*MIN_SCALE);
        scale.setToX(scaleB*MAX_SCALE);
        scale.setToY(scaleB*MAX_SCALE);
    }
}
