/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.TableCell;

import AudioPlayer.tagging.Metadata;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 */
public interface RatingCellFactory extends Callback<TableColumn<Metadata,Double>,TableCell<Metadata,Double>> {
    
}