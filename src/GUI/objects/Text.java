/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * {@link javafx.scene.text.Text} with a default styleclass "text-shape" and 
 * support for automatic wrap width based on contained text value.
 * <p>
 * 
 * @author Plutonium_
 */
public class Text extends javafx.scene.text.Text {
    public static final String STYLECLASS = "text-shape";
   
    public Text() {
        super();
        getStyleClass().add(STYLECLASS);
        
    }
    public Text(String text) {
        super(text);
        getStyleClass().add(STYLECLASS);
    }
    public Text(double x, double y, String text) {
        super(x,y,text);
        getStyleClass().add(STYLECLASS);
    }
    
    /**
     * Sets wrappingWidth to achieve roughly rectangular size. There is a slight
     * horizontal bias (width > height).
     * <p>
     * Use this feature when this Text object is expected to have size dependent
     * on its text and influences the size of its parent. For example popups 
     * displaying text.
     * <p>
     * Invoke after text has been set.
     * <p>
     * The value depends on type and size of the font and might not produce
     * optimal results. Try and see.
     */
    public void wrapWidthNaturally() {
        wrapWidthSetter.changed(null,null,getText());
    }
    
    ChangeListener<String> wrapWidthSetter = (o,ov,nv) -> {
        String s = nv==null ? "" : nv;
        setWrappingWidth(100+s.length()/4);
    };
    
    private final BooleanProperty wrappingWithNatural = new SimpleBooleanProperty(false) {
        @Override
        public void set(boolean newV) {
            super.set(newV);
            if(newV) {
                textProperty().addListener(wrapWidthSetter);
                // fire to initialize
                wrapWidthSetter.changed(null,null,getText());
            }
            else textProperty().removeListener(wrapWidthSetter);
        }
    };
    
    /**
     * Returns natural wrapping width property.
     * @see #setWrappingWidthNatural(boolean)
     * @return 
     */
    public BooleanProperty wrappingWidthNatural() {
        return wrappingWithNatural;
    }
    
    /**
     * Returns value of natural wrapping width property. Default false.
     * @see #setWrappingWidthNatural(boolean)
     * @return 
     */
    public boolean isWrappingWidthNaturally() {
        return wrappingWithNatural.getValue();
    }
    
    /**
     * Sets value of natural wrapping width property.
     * Sets natural wrapping width dynamically reacting on text change on/off. 
     * Default false.
     * @see #wrapWidthNaturally()
     * @param val 
     */
    public void setWrappingWidthNatural(boolean val) {
        wrappingWithNatural.setValue(val);
    }
    
    
    
}
