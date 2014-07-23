
package GUI;

import Action.IsAction;
import Action.IsActionable;
import Configuration.AppliesConfig;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.LayoutAggregators.SwitchPane;
import GUI.objects.Pickers.MoodPicker;
import Layout.Layout;
import Layout.LayoutManager;
import java.io.File;
import static java.io.File.separator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import static javafx.application.Application.STYLESHEET_CASPIAN;
import static javafx.application.Application.STYLESHEET_MODENA;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import javafx.scene.text.FontWeight;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import main.App;
import utilities.FileUtil;
import utilities.Log;
import utilities.Util;

/**
 *
 * @author uranium
 */
@IsActionable
@IsConfigurable
public class GUI implements Configurable {
    
    // properties
    @IsConfig(name = "Font", info = "Application font.")
    public static Font font = Font.getDefault();
    @IsConfig(name = "Skin", info = "Application skin.")
    public static String skin = "Default";
    
    @IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
    public static boolean blur_layoutMode = false;
    @IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
    public static boolean opacity_layoutMode = false;
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.", min=0.0, max=1.0)
    public static double opacity_LM = 0.2;
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur efect intensity.", min=0.0, max=20.0)
    public static double blur_LM = 8;
    @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
    public static double duration_LM = 250;
    

    @IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
    public static boolean snapping = true;
    @IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
    public static double snapDistance = 7;
    @IsConfig(name = "Tab always auto align", info = "Always alignes tabs after tab dragging so single tab covers the screen.")
    public static boolean align_tabs = true;
    @IsConfig(name = "Tab switch min drag distance", info = "Required length of drag at"
            + " which tab switch animation gets activated. Tab switch activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    public static double dragDistance = 350;
    @IsConfig(name = "Tab switch min drag fraction", info = "Required length of drag as"
            + " a fraction of window at which tab switch animation gets activated."
            + " Tab switch activates if at least one condition is fulfilled min "
            + "distance or min fraction.", min=0.0, max=1.0)
    public static double dragFraction = 350;
    
    // other
    public static boolean alt_state = false;
    private static final List<String> skins = new ArrayList();
    
    // 'singleton' objects and controls for use within app
    // TO DO: get rid of this
    public static final MoodPicker MOOD_PICKER = new MoodPicker();
    
/******************************************************************************/
    
    public static void initialize(){
        findSkins();
    }
    
    public static void setLayoutMode(boolean val) {
        if (!val) {
            LayoutManager.getLayouts().forEach(Layout::hide);
            alt_state = false;
        } else {
            LayoutManager.getLayouts().forEach(Layout::show);
            alt_state = true;
        }
    }
    
    public static boolean isLayoutMode() {
        return alt_state;
    }
    /** Loads/refreshes whole gui. */
    @IsAction(name = "Reload GUI.", description = "Reload application GUI. Includes skin, font, layout.", shortcut = "F5")
    public static void refresh() {
        if (App.isInitialized()) {
            applySkin();
            applyFont();
            loadLayout();
            applyAlignTabs();
        }
    }
    
    /** Loads/refreshes active layout. */
    @IsAction(name = "Reload layout", description = "Reload layout.", shortcut = "F6")
    public static void loadLayout() {
        LayoutManager.getLayouts().forEach(l-> {
//            l.close();
            l.load();});
    }
    
    /** Toggles layout controlling mode. */
    @IsAction(name = "Manage Layout", description = "Enables layout managment mode.", shortcut = "F7")
    public static void toggleLayoutMode() {
        setLayoutMode(!alt_state);
    }
    
    @IsAction(name = "Show/Hide application", description = "Equal to switching minimized mode.", shortcut = "CTRL+ALT+W", global = true)
    @IsAction(name = "Minimize", description = "Switch minimized mode.", shortcut = "F9")
    public static void toggleMinimize() {
        Window.getActive().toggleMinimize();
    }
    
    @IsAction(name = "Maximize", description = "Switch maximized mode.", shortcut = "F10")
    public static void toggleMaximize() {
        Window.getActive().toggleMaximize();
    }
    
    @IsAction(name = "Fullscreen", description = "Switch fullscreen mode.", shortcut = "F11")
    public static void toggleFullscreen() {
        Window.getActive().toggleFullscreen();
    }

    
    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    public static void findSkins() {
        // get + verify path
        File dir = App.SKIN_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Search for skins failed." + dir.getPath() + " could not be accessed.");
            return;
        }
        // find skin directories
        File[] dirs = dir.listFiles(File::isDirectory);
        // find & register skins
        Log.mess("Registering external skins.");
        skins.clear();
        for (File d: dirs) {
            String name = d.getName();
            File css = new File(d, name + ".css");
            if(FileUtil.isValidFile(css)) {
                skins.add(name);
                Log.mess("    Skin " + name + " registered.");
            }
        }
        
        if (skins.isEmpty())
            Log.mess("No skins found.");
        else
            Log.mess(skins.size() + " skins found.");
        
        Log.mess("Registering internal skins.");
        skins.add(Util.capitalizeStrong(STYLESHEET_CASPIAN));
        skins.add(Util.capitalizeStrong(STYLESHEET_MODENA));
        Log.mess("    Skin Modena registered.");
        Log.mess("    Skin Caspian registered.");
    }
    
    public static List<String> getSkins() {
       return skins;
    }
    
    

    
/*****************************  setter methods ********************************/
    
    /**
     * Applies specified font on the application. The font can be overridden 
     * locally.
     * The method executes only if the GUI is fully initialized, otherwise does
     * nothing.
     * @param font
     */
    public static void setFont(Font _font) {
        font = _font;
        applyFont();
    }
    
    /**
     * Changes application's skin and applies it.
     * This is a convenience method that constructs a file from the skinname
     * and calls the setSkin(File skin) method.
     * The skin file will be constructed based on the fact that it must pass
     * the isValidSkinFile() check. The path is constructed like this:
     * Application.SkinsPath/skinname/skinname.css
     * For any details regarding the mechanics behind the method see documentation
     * of that method.
     * @param skinname name of the skin to apply.
     */
    public static void setSkin(String skinname) { System.out.println("setting " + skinname + " "+ skin);
        if (skinname == null || skinname.isEmpty() || skinname.equalsIgnoreCase(STYLESHEET_MODENA)) {
            setSkinModena();
        } else if (skinname.equalsIgnoreCase(STYLESHEET_CASPIAN)) {
            setSkinCaspian();
        }
        File skin_file = new File(App.SKIN_FOLDER().getPath(), 
                                    skinname + separator + skinname + ".css");
        setSkinExternal(skin_file);
    }
    /**
     * Changes application's skin and applies it.
     * @param file - css file of the skin to load. It is expected that the skin
     * will have all its external resources ready and usable or it could fail
     * to load properly.
     * To avoid exceptions and errors, isValidSkinFile() check  is ran on this
     * parameter before running this method.
     * @return true if the skin has been applied.
     * False return value signifies that gui has not been initialized,
     * skin file could be loaded or was not valid skin file.
     * True return value doesnt imply successful skin loading, but guarantees
     * that the new skin has been applied regardless of the success.
     */
    private static boolean setSkinExternal(File file) {
        if (App.getWindowOwner().isInitialized() && FileUtil.isValidSkinFile(file)) {
            try {
                String url = file.toURI().toURL().toExternalForm();
                // force refresh skin if already set
                if (skin.equals(FileUtil.getName(file))) {
                    Application.setUserAgentStylesheet(null);
                    Application.setUserAgentStylesheet(url);
                // set skin
                } else {
                    Application.setUserAgentStylesheet(url);
                    // TODO
//                    App.getWindowOwner().getStage().getScene().getRoot().getStylesheets().setAll(url);
                    skin = FileUtil.getName(file);
                }
                return true;
            } catch (MalformedURLException ex) {
                Log.err(ex.getMessage());
                return false;
            }
        }
        return false;
    }
    private static boolean setSkinModena() {
        if (App.getWindowOwner().isInitialized()) {
            Application.setUserAgentStylesheet(STYLESHEET_MODENA);
            skin = "Modena";
            return true;
        }
        return false;
    }
    private static boolean setSkinCaspian() {
        if (App.getWindowOwner().isInitialized()) {
            Application.setUserAgentStylesheet(STYLESHEET_CASPIAN);
            skin = "Caspian";
            return true;
        }
        return false;
    }
    
/****************************  applying methods *******************************/
    
    @AppliesConfig(config = "skin")
    public static void applySkin() {
        setSkin(skin);
    }
    
    @AppliesConfig(config = "font")
    private static void applyFont() {
        // apply only if application initialized correctly
        if (App.isInitialized()) {
            // we need to apply to each window separately
            List<Window> ws = new ArrayList<>(ContextManager.windows);
            ws.forEach(w ->{
                String tmp = font.getStyle().toLowerCase();
                FontPosture style = tmp.contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = tmp.contains("bold") ? BOLD : NORMAL;
                // for some reason javaFX and css values are quite different...
                String styleS = style==ITALIC ? "italic" : "normal";
                String weightS = weight==BOLD ? "bold" : "normal";
                w.getStage().getScene().getRoot().setStyle(
                    "-fx-font-family: \"" + font.getFamily() + "\";" + 
                    "-fx-font-style: " + styleS + ";" + 
                    "-fx-font-weight: " + weightS + ";" + 
                    "-fx-font-size: " + font.getSize() + ";"
                );
            });
        }
    }
    
    @AppliesConfig(config = "align_tabs")
    private static void applyAlignTabs() {
        ContextManager.windows.stream()
                .map(Window::getLayoutAggregator)
                .filter(la->la instanceof SwitchPane)
                .map(la->(SwitchPane)la)
                .forEach(sp -> sp.setAlwaysAlignTabs(align_tabs));
    }
}
