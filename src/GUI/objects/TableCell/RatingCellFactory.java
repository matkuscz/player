/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.TableCell;

import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.tagging.Metadata;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import util.functional.functor.FunctionC;

/**
 */
@IsPluginType
public interface RatingCellFactory extends FunctionC<TableColumn<Metadata,Double>,TableCell<Metadata,Double>> {
    
}
