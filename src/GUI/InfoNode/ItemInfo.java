/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.InfoNode;

import static AudioPlayer.tagging.Cover.Cover.CoverSource.ANY;
import AudioPlayer.tagging.Metadata;
import Layout.Widgets.Features.SongInfo;
import gui.objects.Thumbnail.Thumbnail;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/** Info about song. */
public class ItemInfo extends AnchorPane implements SongInfo {

    @FXML private Label typeL;
    @FXML private Label indexL;
    @FXML private Label songL;
    @FXML private Label artistL;
    @FXML private Label albumL;
    @FXML private AnchorPane infoContainer;
    @FXML private AnchorPane coverContainer;
    private Thumbnail thumb;

    public ItemInfo() {
	this(true);
    }

    public ItemInfo(boolean include_cover) {
	try {
	    FXMLLoader fxmlLoader = new FXMLLoader(ItemInfo.class.getResource("ItemInfo.fxml"));
	    fxmlLoader.setRoot(this);
	    fxmlLoader.setController(this);
	    fxmlLoader.load();

	    if (include_cover) {
		// create
		thumb = new Thumbnail(coverContainer.getPrefHeight());
		coverContainer.getChildren().add(thumb.getPane());
	    } else {
		// remove cover
		getChildren().remove(coverContainer);
		AnchorPane.setLeftAnchor(infoContainer, AnchorPane.getRightAnchor(infoContainer));
		coverContainer = null;
	    }

	} catch (IOException e) {
	    throw new RuntimeException("Could not load ItemInfo.fxml", e);
	}
    }

    // leave the method here as fxml loader might complain
    public void initialize(URL url, ResourceBundle rb) {
	// do nothing
    }

    @Override
    public void setValue(Metadata m) {
        setValue("", m);
    }
    
    /**
     Displays metadata information and title.
     <p>
     @param title
     @param m
     */
    public void setValue(String title, Metadata m) {
	Objects.requireNonNull(m);
	typeL.setText(title);
	if (thumb != null) thumb.loadImage(m.getCover(ANY));
	indexL.setText(m.getPlaylistIndexInfo());
	songL.setText(m.getTitle());
	artistL.setText(m.getArtist());
	albumL.setText(m.getAlbum());
    }

}
