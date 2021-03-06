/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import util.dev.Log;

/**
 * Widget based on .fxml file. Uses {@link FXMLController}.
 * <p>
 * This class wraps any desired .fxml file denoted by its location into widget
 * so it can be recognized by the application as a component for layout.
 * <p>
 * Widget is loaded dynamically from its location. It adopts the standard fxml +
 * controller pattern.
 * More on the creation process of this widget: {@link FXMLWidgetFactory}
 * 
 * @author uranium
 */
public final class FXMLWidget extends Widget<FXMLController> {
    
    FXMLWidget(String name, WidgetFactory factory) {
        super(name,factory);
    }

    @Override
    public Node loadInitial() {
        try {
            controller = getFactory().instantiateController();
            controller.setWidget(this);
            
            FXMLLoader loader = new FXMLLoader();
                       loader.setLocation(getFactory().url);
                       loader.setController(controller);
            Node n = loader.load();
            
            controller.init();
            restoreConfigs();
            controller.refresh();
            
            return n;
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.err("Widget " + name + " failed to load. " + ex.getMessage() );
            // inject empty content
            return Widget.EMPTY().load();
        }
    }

    @Override
    public FXMLWidgetFactory getFactory() {
        return (FXMLWidgetFactory) super.getFactory();
    }
    
    /** Returns location of the widget's parent directory. Never null. */
    public File getLocation() {
        // we need to manually fix encoding so spaces in names dont cause errors
        String s = getFactory().url.getPath().replace("%20"," ");
        return new File(s).getParentFile();
    }

}