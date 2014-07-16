
package GUI;

import Action.Action;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.LastFM.LastFMManager;
import AudioPlayer.tagging.Metadata;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Configuration.ValueConfig;
import Configuration.ValueConfigurable;
import GUI.LayoutAggregators.EmptyLayoutAggregator;
import GUI.LayoutAggregators.LayoutAggregator;
import GUI.LayoutAggregators.UnLayoutAggregtor;
import GUI.objects.ClickEffect;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import static GUI.objects.Window.Resize.E;
import static GUI.objects.Window.Resize.N;
import static GUI.objects.Window.Resize.NE;
import static GUI.objects.Window.Resize.NONE;
import static GUI.objects.Window.Resize.NW;
import static GUI.objects.Window.Resize.S;
import static GUI.objects.Window.Resize.SE;
import static GUI.objects.Window.Resize.SW;
import static GUI.objects.Window.Resize.W;
import Layout.Component;
import Layout.Layout;
import Layout.WidgetImpl.LayoutManagerComponent;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.Widget_Source;
import PseudoObjects.Maximized;
import Serialization.SelfSerializator;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.COLUMNS;
import static de.jensd.fx.fontawesome.AwesomeIcon.GEARS;
import static de.jensd.fx.fontawesome.AwesomeIcon.IMAGE;
import de.jensd.fx.fontawesome.test.IconsBrowser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ALT;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.util.Duration;
import main.App;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;
import utilities.Animation.Interpolators.ElasticInterpolator;
import utilities.Log;
import utilities.Util;

/**
 * Window for application.
 * <p>
 * Window with basic functionality.
 * <p>
 * Below is a code example creating and configuring custom window instance:
 * <pre>
 *     Window w = Window.create();
 *            w.setIsPopup(true);
 *            w.getStage().initOwner(App.getWindow().getStage());
 *            w.setTitle(title);
 *            w.setContent(content);
 *            w.show();
 *            w.setLocationCenter();
 * </pre>
 * 
 * @author plutonium_, simdimdim
 */
@IsConfigurable
public class Window extends WindowBase implements SelfSerializator<Window> {
    
    /**
     * Get focused window. There is only one focused window in the application
     * at once.
     * <p>
     * Use when querying for focused window. 
     * @return focused window or null if none focused.
     */
    public static Window getFocused() {
        return ContextManager.windows.stream()
                .filter(Window::isFocused).findAny().orElse(null);
    }
    /**
     * Same as {@link #getFocused()} but when none focused returns main window
     * instead of null.
     * <p>
     * Both methods are equivalent except for when the application itself has no
     * focus - no window has focus.
     * <p>
     * Use when null must be avoided and the main window substitute for focused
     * window will not cause problem and if this method risk being called when
     * no window has focus.
     * @return focused window or main window if none. Never null.
     */
    public static Window getActive() {
        return ContextManager.windows.stream()
                .filter(Window::isFocused).findAny().orElse(null);
    }
    
/******************************** Configs *************************************/
    @IsConfig(name="Window header visiblility preference", info="Remembers header"
            + " state for both fullscreen and not. When selected 'auto off' is true ")
    public static boolean headerVisiblePreference = true;
    @IsConfig(name = "Opacity", info = "Window opacity.", min=0, max=1)
    public static double windowOpacity = 1;
    @IsConfig(name = "Overlay effect", info = "Use color overlay effect.")
    public static boolean gui_overlay = false;
    @IsConfig(name = "Overlay effect use song color", info = "Use song color if "
            + "available as source color for gui overlay effect.")
    public static boolean gui_overlay_use_song = false;
    @IsConfig(name = "Overlay effect color", info = "Set color for color overlay effect.")
    public static Color gui_overlay_color = Color.BLACK;
    @IsConfig(name = "Overlay effect normalize", info = "Forbid contrast and "
            + "brightness change. Applies only hue portion of the color for overlay effect.")
    public static boolean gui_overlay_normalize = true;
    @IsConfig(name = "Overlay effect intensity", info = "Intensity of the color overlay effect.", min=0, max=1)
    public static double overlay_norm_factor = 0.5;
    
    @AppliesConfig(config = "headerVisiblePreference")
    private static void applyHeaderVisiblePreference() {
        // weird that this still doesnt apply it correctly, whats wrong?
        ContextManager.windows.forEach(w->w.setShowHeader(w.showHeader));
    }
    
    @AppliesConfig(config = "windowOpacity")
    private static void applyWindowOpacity() {
        ContextManager.windows.forEach(w->w.getStage().setOpacity(windowOpacity));
    }
    
    public static void setColorEffect(Color color) {
        gui_overlay_color = color;
        applyColorOverlay();
    }
    
    @AppliesConfig(config = "overlay_norm_factor")
    @AppliesConfig(config = "gui_overlay_use_song")
    @AppliesConfig(config = "gui_overlay_normalize")
    @AppliesConfig(config = "gui_overlay_color")
    @AppliesConfig(config = "gui_overlay")
    public static void applyColorOverlay() {
        if(gui_overlay_use_song) applyOverlayUseSongColor();
        else applyColorEffect(gui_overlay_color);
    }
    private static void applyColorEffect(Color color) {
        if(!App.isInitialized()) return;
        if(gui_overlay) {            
            // normalize color
            if(gui_overlay_normalize)
                color = Color.hsb(color.getHue(), 0.5, 0.5);
            
            final Color cl = color;
            // apply effect
            ContextManager.windows.forEach( w -> {
                w.colorEffectPane.setBlendMode(BlendMode.OVERLAY);
                w.colorEffectPane.setBackground(new Background(new BackgroundFill(cl, CornerRadii.EMPTY, Insets.EMPTY)));
                w.colorEffectPane.setOpacity(overlay_norm_factor);
                w.colorEffectPane.setVisible(true);
            });

        } else {
            // disable effect
            ContextManager.windows.forEach( w -> {
                w.colorEffectPane.setVisible(false);
            });
        }
    }
    
    private static ChangeListener<Metadata> colorListener;
    
    private static void applyOverlayUseSongColor() {
        if(gui_overlay_use_song) {
            // lazily create and add listener
            if(colorListener==null) {
                colorListener = (o,oldV,newV) -> {
                    Color c = newV.getColor();
                    applyColorEffect( c==null ? gui_overlay_color : c);
                };
                Player.currentMetadataProperty().addListener(colorListener);
                // fire upon binding to create immediate response
                colorListener.changed(null, null, Player.getCurrentMetadata());
            } else {
                colorListener.changed(null, null, Player.getCurrentMetadata());
            }            
        }
        else {
            // remove and destroy listener
            if(colorListener!=null)
                Player.currentMetadataProperty().removeListener(colorListener);
            colorListener=null;
            applyColorOverlay();
        }
    }
    
/******************************************************************************/
    
    /** @return new window or null if error occurs during initialization. */
    public static Window create() {
        try {
            Window w = new Window();
            if(App.getWindowOwner()!=null)
                w.getStage().initOwner(App.getWindowOwner().getStage());
            URL fxml = Window.class.getResource("Window.fxml");
            FXMLLoader l = new FXMLLoader(fxml);
                       l.setRoot(w.root);
                       l.setController(w);
                       l.load();
            w.initialize();
            w.minimizeB.setVisible(false);
                   
            return w;
        } catch (IOException ex) {
            Log.err("Couldnt create Window. " + ex.getMessage());
            return null;
        }   
    }
    
/******************************************************************************/
    
    private LayoutAggregator layout_aggregator = new EmptyLayoutAggregator();
    boolean main = false;
    
    @FXML AnchorPane root = new AnchorPane();
    @FXML public AnchorPane content;
    @FXML private HBox controls;

    @FXML Button pinB;
    @FXML Button miniB;
    @FXML Button minimizeB;
    @FXML Pane colorEffectPane;
    public @FXML AnchorPane contextPane;
    public @FXML AnchorPane overlayPane;
    
    private Window() {
        super();
    }
    
    /** Initializes the controller class. */
    private void initialize() {
        getStage().setScene(new Scene(root));
        getStage().setOpacity(windowOpacity);
        
        // avoid some instances of not closing properly
        s.setOnCloseRequest(e->{
            System.out.println("CLOSING WINDOW REQUEST");
            close();
        });
        
        // root is assigned a 'window' styleclass to allow css skinning
        // set 'focused' pseudoclass for window styleclass
        PseudoClass focused = PseudoClass.getPseudoClass("focused");
        s.focusedProperty().addListener((o,oldV,newV)->{
            root.pseudoClassStateChanged(focused, newV);
        });
        // add to list of active windows
        ContextManager.windows.add(this);
        // set shortcuts
        Action.getActions().values().stream().filter(a->!a.isGlobal())
                .forEach(Action::register);
        
        root.addEventFilter(MOUSE_MOVED, e -> {
            if (ClickEffect.trail_effect) 
                ClickEffect.run(e.getSceneX(), e.getSceneY());
        });
        root.addEventFilter(MOUSE_PRESSED,  e -> {
            // update coordinates for context manager
            ContextManager.setX(e.getSceneX());
            ContextManager.setY(e.getSceneY());
            if (!ClickEffect.trail_effect) 
                ClickEffect.run(e.getSceneX(), e.getSceneY());
            ContextManager.closeMenus();
            appDragStart(e);
        });
        
        root.addEventFilter(MOUSE_RELEASED, e -> app_drag = false );
        
        root.addEventHandler(MOUSE_DRAGGED,e -> {
            if (e.getButton()==MouseButton.PRIMARY) {
                if(app_drag)
                    appDragDo(e);
                else
                    appDragStart(e);
            }
        });
        
        // header double click maximize, show header on/off
        header.setOnMouseClicked( e -> {
            if(e.getButton()==MouseButton.PRIMARY) {
                if(e.getClickCount()==2)
                    toggleMaximize();
            }
            if(e.getButton()==MouseButton.SECONDARY) {
                if(e.getClickCount()==2)
                    setShowHeader(!showHeader);
            }
        });
        
        // change volume on scroll
        // if some component has its own onScroll behavior, it should consume
        // the event so this one will not fire
        root.setOnScroll( e -> {
            if(e.getDeltaY()>0) PLAYBACK.incVolume();
            else if(e.getDeltaY()<0) PLAYBACK.decVolume();
        });
        
        
        Rectangle r = new Rectangle(15, 15, Color.BLACK);
                  r.widthProperty().bind(controls.widthProperty().subtract(20));
                  r.heightProperty().bind(controls.heightProperty());
                  r.relocate(controls.getLayoutX()+controls.getWidth()-20,controls.getLayoutY());
//        controls.setClip(r);
        
        TranslateTransition t = new TranslateTransition(Duration.millis(450), r);
                            t.setInterpolator(new ElasticInterpolator(EASE_OUT));
        header.setOnMouseEntered(e -> {
//            t.stop();
            t.setByX(-100);
            t.play();
        });
        header.setOnMouseExited(e -> {
//            t.stop();
            t.setToX(100);
            t.play();
        });
    };
    
    public void setAsMain() {
        if (App.getWindow()!=null)
            throw new RuntimeException("Only one window can be main");
        main = true;
        
        setIcon(App.getIcon());
        setTitle(App.getAppName());
        setTitlePosition(Pos.CENTER_LEFT);
        miniB.setVisible(true);
        minimizeB.setVisible(true);
        new ContextManager(overlayPane,contextPane);
        App.window = this;
    }
    
/******************************* CONTENT **************************************/
    
    public void setContent(Node n) {
        content.getChildren().clear();
        content.getChildren().add(n);
        AnchorPane.setBottomAnchor(n, 0.0);
        AnchorPane.setRightAnchor(n, 0.0);
        AnchorPane.setLeftAnchor(n, 0.0);
        AnchorPane.setTopAnchor(n, 0.0);
    }
    
    public void setContent(Component c) {
        Layout l = new Layout();
        // also sets parent pane for layout
        LayoutAggregator la = new UnLayoutAggregtor(l);
               // set child after parent pane is set for layout 
               l.setChild(c);
        setLayoutAggregator(la);
    }
    
    public void setLayoutAggregator(LayoutAggregator la) {
        Objects.requireNonNull(la);
        // clear previous content
        layout_aggregator.getLayouts().forEach(Layout::close);
        // set new content
        layout_aggregator = la;
        setContent(layout_aggregator.getRoot());
        // load new content
        layout_aggregator.getLayouts().forEach(Layout::load);
    }
    
    /**
     * Returns layout aggregator of this window.
     * @return layout aggregator, never null.
     */
    public LayoutAggregator getLayoutAggregator() {
        return layout_aggregator;
    }
    
/******************************    HEADER & BORDER    **********************************/
    
    @FXML private BorderPane header;
    @FXML private Region lBorder;
    @FXML private Region rBorder;
    @FXML private ImageView iconI;
    @FXML private Label titleL;
    @FXML private HBox leftHeaderBox;
    private boolean showHeader = true;
    
    /**
     * Sets visibility of the window header, including its buttons for control
     * of the window (close, etc).
     */
    public void setShowHeader(boolean val) {
        showHeader = val;
        showHeader(val);
    }
    private void showHeader(boolean val) {
        controls.setVisible(val);
        leftHeaderBox.setVisible(val);
        if(val) {
            header.setPrefHeight(25);
            AnchorPane.setTopAnchor(content, 25d);
            AnchorPane.setTopAnchor(lBorder, 25d);
            AnchorPane.setTopAnchor(rBorder, 25d);
        } else {
            header.setPrefHeight(5);
            AnchorPane.setTopAnchor(content, 5d);
            AnchorPane.setTopAnchor(lBorder, 5d);
            AnchorPane.setTopAnchor(rBorder, 5d);
        }
    }
    
    /** Set title for this window shown in the header.*/
    public void setTitle(String text) {
        titleL.setText(text);
    }
    
    /** Set title alignment. */
    public void setTitlePosition(Pos align) {
        BorderPane.setAlignment(titleL, align);
    }
    
    /** Set icon. Null clears. */
    public void setIcon(Image img) {
        iconI.setImage(img); 
       leftHeaderBox.getChildren().remove(iconI);
//       if(img!=null)leftHeaderBox.getChildren().add(0, iconI);
       leftHeaderBox.getChildren().add(0, AwesomeDude.createIconLabel(AwesomeIcon.MUSIC,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(2, AwesomeDude.createIconLabel(AwesomeIcon.GITHUB,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(3, AwesomeDude.createIconLabel(AwesomeIcon.GITHUB_ALT,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(4, AwesomeDude.createIconLabel(AwesomeIcon.GITHUB_SQUARE,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(4, AwesomeDude.createIconLabel(AwesomeIcon.GITTIP,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(1, AwesomeDude.createIconLabel(AwesomeIcon.CROSSHAIRS,"","15","11", CENTER));
       leftHeaderBox.getChildren().add(2, AwesomeDude.createIconLabel(AwesomeIcon.REFRESH,"","15","11", CENTER));
       
       // icon button - show all available FontAwesome icons in a popup
        Label iconsB = AwesomeDude.createIconLabel(IMAGE,"","15","11",CENTER);
              iconsB.setOnMouseClicked( e ->{
                IconsBrowser ib = new IconsBrowser();
                PopOver p = new PopOver(ib);
                        p.show(iconsB);
              });
              iconsB.setTooltip(new Tooltip("Icon browser (developing tool)"));
        // settings button - show application settings in a popup
        Label propB = AwesomeDude.createIconLabel(GEARS,"","15","11",CENTER);
              propB.setOnMouseClicked( e ->
                  WidgetManager.getWidget(ConfiguringFeature.class,Widget_Source.FACTORY)
              );
              propB.setTooltip(new Tooltip("Application settings"));
        // manage layout button - sho layout manager in a popp
        Label layB = AwesomeDude.createIconLabel(COLUMNS,"","15","11",CENTER);
              layB.setOnMouseClicked( e ->
                  ContextManager.showFloating(new LayoutManagerComponent().getPane(), "Layout Manager")
              );
              layB.setTooltip(new Tooltip("Manage layouts"));
        // lasFm button - show basic lastFm settings nd toggle scrobbling on/off 
              // create graphics once
        Image lastFMon = Util.loadImage(new File("lastFMon.png"), 30);
        Image lastFMoff = Util.loadImage(new File("lastFMoff.png"), 30);
        ImageView lastFMview = new ImageView();
                  lastFMview.setFitHeight(15);
                  lastFMview.setFitWidth(15);         
                  lastFMview.setPreserveRatio(true);
            // maintain proper icon
        ChangeListener<Boolean> fmlistener = (o, oldV, newV)->
              lastFMview.setImage(newV ? lastFMon : lastFMoff);
        LastFMManager.scrobblingEnabledProperty().addListener(fmlistener);
            // initialize proper icon
        fmlistener.changed(null, false, LastFMManager.getScrobblingEnabled());
        
        Label lastFMB = new Label("",lastFMview);
        lastFMB.setOnMouseClicked( e ->{
            if(e.getButton() == MouseButton.PRIMARY){
                if(LastFMManager.getScrobblingEnabled()){
                    LastFMManager.toggleScrobbling();
                } else {
                    if(LastFMManager.isLoginSet()){
                        LastFMManager.toggleScrobbling();
                    } else {
                        SimpleConfigurator lfm = new SimpleConfigurator(
                            new ValueConfigurable(
                                new ValueConfig("Username", LastFMManager.acquireUserName()),
                                new ValueConfig("Password", LastFMManager.getHiddenPassword())                                  
                            ), vc->{ LastFMManager.saveLogin(
                                (String)vc.getFields().get(0).getValue(),
                                (String)vc.getFields().get(1).getValue());                                              
                        });
                        PopOver p = new PopOver("LastFM login", lfm);
                        p.show(lastFMB);
                    }
                }
            }
        }

        );
        lastFMB.setTooltip(new Tooltip("Manage layouts"));
              
         leftHeaderBox.getChildren().addAll(iconsB,layB,propB,lastFMB);
    }
    
/**************************** WINDOW MECHANICS ********************************/
    
//    private boolean isPopup = false;
//    private boolean autoclose = true;
//    
//    /** 
//     * Set false to get normal behavior. Set true to enable popup-like autoclosing
//     * that can be turned off. Setting this to false will cause autoclosing
//     * value be ignored. Default false;
//     */
//    public void setIsPopup(boolean val) {
//        isPopup = val;
//        pinB.setVisible(val);
//    }
//    public boolean isPopup() {
//        return isPopup;
//    }
//    /**
//     * Set autoclose functinality. If isPopup==false this property is ignored.
//     * False is standard window behavior. Setting
//     * true will cause the window to close on mouse click occuring anywhere
//     * within the application and outside of this window - like popup. Default
//     * is false.
//     */
//    public void setAutoClose(boolean val) {
//        autoclose = val;
//    }
//    public boolean isAutoClose() {
//        return autoclose;
//    }
//    public void toggleAutoClose() {
//        autoclose = !autoclose;
//    }

    @Override
    public void close() {
        // serialize windows if this is main app window
        if(main) WindowManager.serialize();
        // close content- if not main window, it would prevent Layouts form serializing
        // main window closes the app so closing layouts does not play role
        if(!main) layout_aggregator.getLayouts().forEach(Layout::close);
        // remove from window list as life time of this ends
        ContextManager.windows.remove(this); 
        if(main) {
            // close all pop overs first (or we risk an exception and not closing app
            // properly - PopOver bug
            // also have to create new list or we risk ConcurrentModificationError
            new ArrayList<>(PopOver.active_popups).forEach(PopOver::hideImmediatelly);
            // act as main window and close whole app
            App.getWindowOwner().close();
        }
        // in the end close itself
        super.close();
    }
    
//    /**
//     * Closes window if (isPopup && autoclose) evaluates to true. This method is
//     * designed specifically for auto-closing functionality.
//     */
//    public void closeWeak() {
//        if(isPopup && autoclose) close(); 
//    }

    
   @Override  
    public void setFullscreen(boolean val) {  
        super.setFullscreen(val);
        if(headerVisiblePreference){
            if(val)showHeader(false);
            else showHeader(showHeader);
        } else {
            setShowHeader(!showHeader);
        }
    }
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }

    
/*********************************    DRAGGING   ******************************/
    
    private double appX;
    private double appY;
    private boolean app_drag = false;
    
    private void appDragStart(MouseEvent e) {
        app_drag = true;
        appX = e.getSceneX();
        appY = e.getSceneY();
    }
    private void appDragDo(MouseEvent e) {
        if(!app_drag)return;
        
        double SW = Screen.getPrimary().getBounds().getWidth(); //screen_width
        double SH = Screen.getPrimary().getBounds().getHeight(); //screen_height
        double X = e.getScreenX();
        double Y = e.getScreenY();
        double SH5 = SH/5;
        double SW5 = SW/5;
        
        if (isMaximised() == Maximized.NONE)
            setLocation(X - appX, Y - appY);
        
        // (imitate Aero Snap)
        Maximized to;
        
        if (X <= 0 ) {
            if (Y<SH5) to = Maximized.LEFT_TOP;
            else if (Y>SH-SH5) to = Maximized.LEFT_BOTTOM;
            else to = Maximized.LEFT;
        } else if (X<SW5) {
            if(Y<=0) to = Maximized.LEFT_TOP;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.LEFT_BOTTOM;
        } else if (X<SW-SW5) {
            if(Y<=0) to = Maximized.ALL;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.NONE;
        } else if (X<SW-1) {
            if(Y<=0) to = Maximized.RIGHT_TOP;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.RIGHT_BOTTOM;
        } else {
            if (Y<SH5) to = Maximized.RIGHT_TOP;
            else if (Y<SH-SH5) to = Maximized.RIGHT;
            else to = Maximized.RIGHT_BOTTOM;
        }
                
        if(isMaximised() != to) setMaximized(to);
    }
    private void appDragEnd() {
        app_drag = false;
    }

    
/*******************************    RESIZING    *******************************/
        
    @FXML
    private void border_onDragStart(MouseEvent e) {
        double X = e.getSceneX();
        double Y = e.getSceneY();
        double WW = getWidth();
        double WH = getHeight();
        double L = 18; // corner treshold

        if ((X > WW - L) && (Y > WH - L)) {
            is_being_resized = SE;
        } else if ((X < L) && (Y > WH - L)) {
            is_being_resized = SW;
        } else if ((X < L) && (Y < L)) {
            is_being_resized = NW;
        } else if ((X > WW - L) && (Y < L)) {
            is_being_resized = NE;
        } else if ((X > WW - L)) {
            is_being_resized = E;
        } else if ((Y > WH - L)) {
            is_being_resized = S;
        } else if ((X < L)) {
            is_being_resized = W;
        } else if ((Y < L)) {
            is_being_resized = N;
        }
        e.consume();
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        is_being_resized = NONE;
        e.consume();
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if (is_being_resized == SE) {
            setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
        } else if (is_being_resized == S) {
            setSize(getWidth(), e.getScreenY() - getY());
        } else if (is_being_resized == E) {
            setSize(e.getScreenX() - getX(), getHeight());
        } else if (is_being_resized == SW) {
            setSize(getX()+getWidth()-e.getScreenX(), e.getScreenY() - getY());
            setLocation(e.getScreenX(), getY());
        } else if (is_being_resized == W) {
            setSize(getX()+getWidth()-e.getScreenX(), getHeight());
            setLocation(e.getScreenX(), getY());
        } else if (is_being_resized == NW) {
            setSize(getX()+getWidth()-e.getScreenX(), getY()+getHeight()-e.getScreenY());
            setLocation(e.getScreenX(), e.getScreenY());
        } else if (is_being_resized == N) {
            setSize(getWidth(), getY()+getHeight()-e.getScreenY());
            setLocation(getX(), e.getScreenY());
        } else if (is_being_resized == NE) {
            setSize(e.getScreenX() - getX(), getY()+getHeight()-e.getScreenY());
            setLocation(getX(), e.getScreenY());
        }
        e.consume();
    }
    
/******************************************************************************/
    
    @FXML
    private void consumeMouseEvent(MouseEvent e) {
        e.consume();
    }

    @FXML  
    private void entireArea_OnKeyPressed(KeyEvent e) {  
        if (e.getCode().equals(KeyCode.getKeyCode(Action.Shortcut_ALTERNATE)))  
            GUI.setLayoutMode(true);  
        if (e.getCode()==ALT )  
            showHeader(true);  
    }  

    @FXML  
    private void entireArea_OnKeyReleased(KeyEvent e) {
        if (e.getCode().equals(KeyCode.getKeyCode(Action.Shortcut_ALTERNATE)))  
            GUI.setLayoutMode(false);  
        if (e.getCode()==ALT)  
            if(headerVisiblePreference){
                if(isFullscreen()) showHeader(false); 
                else showHeader(showHeader);
            } else {
                showHeader(showHeader);
            }
    }

    @Override
    public void serialize(Window w, File f) throws IOException {
        XStream xstream = new XStream(new DomDriver());
        xstream.autodetectAnnotations(true);
        xstream.registerConverter(new Window.WindowConverter());
        xstream.toXML(w, new BufferedWriter(new FileWriter(f)));
    }

    public static Window deserialize(File f) throws IOException {
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.registerConverter(new Window.WindowConverter());
            return (Window) xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            throw new IOException();
        }
    }
    public static Window deserializeSuppressed(File f) {
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.registerConverter(new Window.WindowConverter());
            return (Window) xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
    
    public static final class WindowConverter implements Converter {
        
        @Override
        public boolean canConvert(Class type) {
            return Window.class.equals(type);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            Window w = (Window) value;
            writer.startNode("W");
            writer.setValue(w.WProp.getValue().toString());
            writer.endNode();
            writer.startNode("H");
            writer.setValue(w.HProp.getValue().toString());
            writer.endNode();
            writer.startNode("X");
            writer.setValue(w.XProp.getValue().toString());
            writer.endNode();
            writer.startNode("Y");
            writer.setValue(w.YProp.getValue().toString());
            writer.endNode();
            writer.startNode("minimized");
            writer.setValue(w.s.iconifiedProperty().getValue().toString());
            writer.endNode();
            writer.startNode("maximized");
            writer.setValue(w.MaxProp.getValue().toString());
            writer.endNode();
            writer.startNode("fullscreen");
            writer.setValue(w.FullProp.getValue().toString());
            writer.endNode();
            writer.startNode("resizable");
            writer.setValue(w.s.resizableProperty().getValue().toString());
            writer.endNode();
            writer.startNode("alwaysOnTop");
            writer.setValue(w.s.alwaysOnTopProperty().getValue().toString());
            writer.endNode();
            writer.startNode("layout-aggregator-type");
            writer.setValue(w.layout_aggregator.getClass().getName());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Window w = Window.create();
            if(w==null) return null;
            
            reader.moveDown();
            w.WProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.HProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.XProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.YProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.s.setIconified(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.MaxProp.set(Maximized.valueOf(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.FullProp.set(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.s.setResizable(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.setAlwaysOnTop(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            reader.getValue(); // ignore
            reader.moveUp();
            
            return w;
        }
    }

}