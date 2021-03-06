
package Layout.Widgets;

import Configuration.CompositeConfigurable;
import Configuration.Configurable;
import Configuration.IsConfig;
import Layout.Component;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.ObjectStreamException;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.Collection;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import util.dev.Log;

/**
 * Widget is an abstract representation of graphical component that works like
 * an individual self sufficient modul.
 * <p>
 * Widgets allows a way to handle variety of different components in defined
 * way. It provides necessary logic to wrap the component, which includes such
 * things as id, name, controller or properties
 * <p>
 * Widget is a wrapper of underlying component exhibiting standalone
 * functionality. Its not its role to handle behavior of the component, but it
 * does provide a way to access it by forwarding its controller. Controller is the
 * object responsible for the component's behavior.
 * The controller is instantiated when widget loads. The controller should
 * not be called before that happens.
 * <p>
 * The Concrete implementation of Widget should make sure it guarantees the
 * existence of Controller after the widget has been loaded. This should be
 * done inside load() method.
 * <p>
 * Always use EMPTY widget instead null
 * 
 * @author uranium
 */
public abstract class Widget<C extends Controller> extends Component implements CompositeConfigurable<Object> {
    
    // Name of the widget. Permanent. same as factory name
    // it needs to be declared to support deserialization
    final String name;
    
    @XStreamOmitField private WidgetFactory factory;
    @XStreamOmitField protected C controller;
    @XStreamOmitField private Node root;  // loaded only once
    
    // configuration
    @XStreamOmitField
    @IsConfig(name = "Is preferred", info = "Prefer this widget among all widgets of its type. If there is a request "
            + "for widget, preferred one will be selected. ")
    private boolean preferred = false;
    
    @XStreamOmitField
    @IsConfig(name = "Is ignored", info = "Ignore this widget if there is a request.")
    private boolean forbid_use = false;
    
    // list of properties of the widget to provide serialisation support since
    // controller doesnt serialise - this is unwanted and should be handled better
    public Map<String,String> configs;
    
    
    /**
     * @param {@link Widget#name}
     */
    public Widget(String name, WidgetFactory factory) {
        this.name = name;
        this.factory = factory;
    }
        
    /** {@inheritDoc} */
    @Override
    public String getName() { return name; }
    
    /**
     * Loads this widget's content.
     * The content is cached and will only be loaded once.
     * Any subsequent call of this method will simply reattach the content to
     * new location in the sceneGraph.
     * <p>
     * Only if the load is initial, the controller will be refreshed.
     * <p>
     * Widget's controller will be null until the first time the widget loads.
     * {@inheritDoc}
     * @return 
     */
    @Override
    public final Node load() {
        // - loads only once
        // - attaching root to the scenegraph will automatically remove it
        //   from its old location
        // - this guarantees that widget loads only once, which means:
        //   - graphics will be constructed only once
        //   - -||- controller, controller will always be in control of
        //     the correct graphics - normally we would have to load both
        //     graphics and controller multiple times because we can not
        //     assign new graphics to old controller
        // - entire state of the widget is intact with the exception of
        //   initial load at deserialisation.
        //   This also makes deserialisation the only time when configs
        //   need to be taken care of manually
        if(root==null) root = loadInitial();
        return root;
    }
    
    /**
     * Loads the widget. Solely used for {@link #load()} method called initially,
     * before the loaded content is cached. Should be called only once per life
     * cycle of the widget and internally.
     * @return 
     */
    protected abstract Node loadInitial();
    
    /**
     * Returns controller of the widget. It provides access to public behavior
     * of the widget.
     * <p>
     * The controller is instantiated when widget loads. The controller should
     * not be used (and needed) before that happens.
     * <p>
     * Do not check the output of this method for null! Receiving null implies
     * wrong use of this method (with the exception of internal use)
     * 
     * @return controller of the widget or null if widget has not been loaded
     * yet.
     */
    public C getController() {
        if(controller == null)
            Log.warn("Possible illegal call. Widget doesnt have a controller yet.");
        return controller;
    }
    
    /** @return whether this widget will be preferred over other widgets */
    public boolean isPreffered() {
        return preferred;
    }
    /** @param val whether this widget will be preferred over other widgets */
    public void setPreferred(boolean val) {
        preferred = val;
    }
    /** @return whether this widget will be ignored on widget request */
    public boolean isIgnored() {
        return forbid_use;
    }
    /** @param val whether this widget will be ignored on widget request */
    public void setIgnored(boolean val) {
        forbid_use = val;
    }
    
    /** @return factory that produces this widget */
    public WidgetFactory getFactory() {
        return factory;
    }
    
    /** @return factory information about this widget */
    public WidgetInfo getInfo() {
        return factory;
    }
    
/******************************************************************************/

    
    /**
     * Returns whether this widget is empty.
     * <p>
     * Empty widget has no graphics. {@link Controller#isEmpty()} indicates 
     * state - that there is no data to display within widget's graphics.
     */
    public boolean isEmpty() {
        return this instanceof EmptyWidget;
    }
    
    /**
     * Returns singleton list containing the controller of this widget
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Collection<Configurable<Object>> getSubConfigurable() {
        return singletonList(controller);
    }    
    
    // the following two methods help with serialising the widget settings
    public void rememberConfigs() {
        if(controller != null) {
            configs = new HashMap();
            getFields().forEach(c -> configs.put(c.getName(), c.getValueS()));
        }
    }
    public void restoreConfigs() {
        if(configs != null) {
            configs.forEach(this::setField);
            configs = null;
        }
    }

    /** @return name of the widget */
    @Override
    public String toString() {
        return name;
    }
    
    
    /**
     * @return empty widget. Use to inject fake widget instead null value.
     */
    public static Widget EMPTY() {
        return new EmptyWidget();
    }
    
    /**
    * There is one major flaw in XStream. Unfortunately it has no way of
    * telling if a field or attribute should get any default value if not
    * present in the xml file. Because constructor is not being invoked we
    * cannot set the value there. Neither setting the value in field definition
    * will work. The resulting instance will always have zero or null values in
    * the fields.
    *
    * The only way of setting the desired default value is using the following
    * method. It is called during deserialization process and here we can check
    * if the field value is null. If yes it means that it's tag is not present
    * and we can set the default value if needed.
    *
    * @return this
    * @throws ObjectStreamException
    */
    // must not be private or it wont get inherited and is called on sublass
    // apparently this isnt inherited or something...
    protected Object readResolve() throws ObjectStreamException {
        // assign factory
        if (factory==null) factory = WidgetManager.getFactory(name);
        // if none exists (maybe the widget was serialized in different application)
        // return null - widget can not exist without its factory
        // the null does not cause exception, it is treated by container as no 
        // component - basically we ignore this widget
        // return factory==null ? null : this;
        
        // using empty widget might be better idea?
        // maybe creating an 'error' widget would be a good idea
        return factory==null ? Widget.EMPTY().load() : this;
    }
    
/******************************************************************************/

    /** Widget metadata. Passed from code to program. Use on controller class. */
    @Retention(value = RUNTIME)
    @Target(value = TYPE)
    public static @interface Info {

        /**
         * Name of the widget. "" by default.
         */
        String name() default "";

        /**
         * Description of the widget.
         */
        String description() default "";

        /**
         * Version of the widget
         */
        String version() default "";

        /**
         * Author of the widget
         */
        String author() default "";

        /**
         * Main developer of the widget.
         * @return
         */
        String programmer() default "";

        /**
         * Co-developer of the widget.
         */
        String contributor() default "";

        /**
         * Last time of change.
         * @return
         */
        String year() default "";

        /**
         * How to use text.
         * <pre>
         * For example:
         * "Available actions:\n" +
         * "    Drag cover away : Removes cover\n" +
         * "    Drop image file : Adds cover\n" +
         * "    Drop audio files : Adds files to tagger\n" +
         * "    Write : Saves the tags\n" +
         * "    Open list of tagged items"
         * </pre>
         * @return
         */
        String howto() default "";

        /**
         * Any words from the author, generally about the intention behind or bugs
         * or plans for the widget or simply unrelated to anything else information.
         * <p>
         * For example: "To do: simplify user interface." or: "Discontinued."
         * @return
         */
        String notes() default "";

        /**
         * Group the widget should categorize under as. Default {@link Widget.Group.UNKNOWN}
         * @return
         */
        Widget.Group group() default Widget.Group.UNKNOWN;
    }
    
    /** Widget's intended functionality. */
    public enum Group {
        APP,
        PLAYBACK,
        PLAYLIST,
        TAGGER,
        LIBRARY,
        VISUALISATION,
        OTHER,
        DEVELOPMENT,
        UNKNOWN;
    }
}