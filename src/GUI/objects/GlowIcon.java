/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
// */

package gui.objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import util.Animation.Anim;

/**
 *
 * @author Plutonium_
 */
public class GlowIcon extends Icon {
    
    public static final String STYLECLASS = "glow-icon";
    
    public GlowIcon(FontAwesomeIconName i, int size) {
        super(i,size);
        getGraphic().getStyleClass().setAll(STYLECLASS);
        getStyleClass().clear();
        applyCss();
        
        DropShadow b = new DropShadow(6, Color.rgb(130,170,255));
        b.setInput(new BoxBlur(1,1,1));
        setEffect(b);
        
        Anim ia = new Anim(at -> b.setRadius(6+13*at)).intpl(x->x*x*x).dur(150);
        Anim oa = new Anim(at -> b.setRadius(6+13*(1-at))).intpl(x->x*x*x).dur(450).delay(150);
        
        // react on hover
        hoverProperty().addListener((ob,ov,nv) -> {
            if(nv) {
                oa.stop();
                ia.playFrom((b.getRadius()-6)/15);
            } else {
                ia.stop();
                oa.playFrom(1-(b.getRadius()-6)/15);
            }
        });
    }
}