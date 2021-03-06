package FileInfo;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Cover.Cover;
import AudioPlayer.tagging.Cover.Cover.CoverSource;
import static AudioPlayer.tagging.Cover.Cover.CoverSource.ANY;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.EMPTY;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import gui.Panes.ImageFlowPane;
import gui.objects.ActionChooser;
import gui.objects.Icon;
import gui.objects.Rater.Rating;
import gui.objects.Thumbnail.ChangeableThumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.SongInfo;
import Layout.Widgets.Widget;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.PLAYING;
import de.jensd.fx.glyphs.GlyphIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.io.File;
import static java.lang.Double.max;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import javafx.scene.Node;
import javafx.scene.control.*;
import static javafx.scene.control.OverrunStyle.ELLIPSIS;
import javafx.scene.image.Image;
import static javafx.scene.input.DragEvent.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import main.App;
import org.reactfx.Subscription;
import static util.File.FileUtil.copyFileSafe;
import static util.File.FileUtil.copyFiles;
import util.File.ImageFileFormat;
import util.Util;
import util.access.Accessor;
import static util.async.Async.*;
import util.async.future.Fut;
import util.graphics.drag.DragUtil;
import static util.graphics.drag.DragUtil.hasImage;

/**
 * File info widget controller.
 * <p>
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "File Info",
    description = "Displays information about a song and cover. Supports rating change.",
    howto = "Available actions:\n" +
            "    Cover left click : Toggles show fields\n" +
            "    Cover left click : Opens over context menu\n" +
            "    Rater left click : Rates displayed song\n" +
            "    Rater right click : Toggles rater skin\n" +
            "    Drag&Drop audio : Displays information for the first dropped item\n" +
            "    Drag&Drop image on cover: Copies images into current item's locaton\n" +
            "    Drag&Drop image elsewhere: Copies images into current item's locaton as cover\n",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class FileInfoController extends FXMLController implements SongInfo {
    
    @FXML AnchorPane entireArea;
    ImageFlowPane layout;
    TilePane tiles = new TilePane(VERTICAL,10,0);
    
    private final Label title = new Label(); 
    private final Label track = new Label(); 
    private final Label disc = new Label(); 
    private final Label gap1 = new Label(); 
    private final Label artist = new Label(); 
    private final Label album = new Label();    
    private final Label album_artist = new Label(); 
    private final Label year = new Label(); 
    private final Label genre = new Label(); 
    private final Label composer = new Label(); 
    private final Label publisher = new Label(); 
    private final Label gap2 = new Label(); 
    private final Label rating = new Label(); 
    private final Label playcount = new Label(); 
    private final Label comment = new Label(); 
    private final Label category = new Label(); 
    private final Label gap3 = new Label(); 
    private final Label filesize = new Label(); 
    private final Label filename = new Label(); 
    private final Label format = new Label(); 
    private final Label bitrate = new Label(); 
    private final Label encoding = new Label(); 
    private final Label location = new Label(); 
    private final Rating rater = new Rating();
    
    private List<Label> labels;
    private final List<Label> visible_labels = new ArrayList();
    private Subscription dataMonitoring;
    private Metadata data;
    
    // auto applied configurable values    
    @IsConfig(name = "Column width", info = "Minimal width for field columns.")
    public final Accessor<Double> minColumnWidth = new Accessor<>(150.0, v -> resize(tiles.getWidth(), tiles.getHeight()));
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public final Accessor<CoverSource> cover_source = new Accessor<>(ANY, this::setCover);
    @IsConfig(name = "Text clipping method", info = "Style of clipping text when too long.")
    public final Accessor<OverrunStyle> overrun_style = new Accessor<>(ELLIPSIS, v -> labels.forEach(l->l.setTextOverrun(v)));
    @IsConfig(name = "Show cover", info = "Show cover.")
    public final Accessor<Boolean> showCover = new Accessor<>(true, this::setCoverVisible);
    @IsConfig(name = "Show fields", info = "Show fields.")
    public final Accessor<Boolean> showFields = new Accessor<>(true, v -> layout.setShowContent(v));
    @IsConfig(name = "Item source", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(PLAYING, v -> dataMonitoring = Player.subscribe(v,dataMonitoring, this::setValue));
    @IsConfig(name = "Show empty fields", info = "Show empty fields.")
    public final Accessor<Boolean> showEmptyFields = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Group fields", info = "Use gaps to separate fields into group.")
    public final Accessor<Boolean> groupFields = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show title", info = "Show this field.")
    public final Accessor<Boolean> showTitle = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show track", info = "Show this field.")
    public final Accessor<Boolean> showtrack = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show disc", info = "Show this field.")
    public final Accessor<Boolean> showdisc = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show artist", info = "Show this field.")
    public final Accessor<Boolean> showartist = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show album artist", info = "Show this field.")
    public final Accessor<Boolean> showalbum_artist = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show album", info = "Show this field.")
    public final Accessor<Boolean> showalbum = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show year", info = "Show this field.")
    public final Accessor<Boolean> showyear = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show genre", info = "Show this field.")
    public final Accessor<Boolean> showgenre = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show composer", info = "Show this field.")
    public final Accessor<Boolean> showcomposer = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show publisher", info = "Show this field.")
    public final Accessor<Boolean> showpublisher = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show rating", info = "Show this field.")
    public final Accessor<Boolean> showrating = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show playcount", info = "Show this field.")
    public final Accessor<Boolean> showplaycount = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show comment", info = "Show this field.")
    public final Accessor<Boolean> showcomment = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show category", info = "Show this field.")
    public final Accessor<Boolean> showcategory = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show filesize", info = "Show this field.")
    public final Accessor<Boolean> showfilesize = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show filename", info = "Show this field.")
    public final Accessor<Boolean> showfilename = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show format", info = "Show this field.")
    public final Accessor<Boolean> showformat = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show bitrate", info = "Show this field.")
    public final Accessor<Boolean> showbitrate = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show encoding", info = "Show this field.")
    public final Accessor<Boolean> showencoding = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show location", info = "Show this field.")
    public final Accessor<Boolean> showlocation = new Accessor<>(true, v -> setVisibility());
    
    // non appliable configurable values
    @IsConfig(name = "Allow no content", info = "Otherwise shows previous content when the new content is empty.")
    public boolean allowNoContent = false;
    
    @Override
    public void init() {
        // initialize gui
        ChangeableThumbnail cover = new ChangeableThumbnail();
                  cover.setBackgroundVisible(false);
                  cover.setBorderToImage(false);
                  cover.onFileDropped = f -> {
                      actPane.item = f;
                      getArea().setActivityVisible(true);
                  };
                  
        layout = new ImageFlowPane(entireArea, cover);
        layout.setMinContentWidth(200);
        layout.setMinContentHeight(120);
        layout.setGap(5);
        
        // set autosizing for tiles to always fill the grid entirely
        tiles.widthProperty().addListener((o,ov,nv) -> resize(nv.doubleValue(), tiles.getHeight()));
        tiles.heightProperty().addListener((o,ov,nv) -> resize(tiles.getWidth(), nv.doubleValue()));
        
        // alight tiles from left top & tile content to center left
        tiles.setAlignment(TOP_LEFT);
        tiles.setTileAlignment(CENTER_LEFT);
        
        // add rater stars to rating label as graphics
        rating.setGraphic(rater);
        rating.setContentDisplay(ContentDisplay.RIGHT);
        
        // grab fields
        labels = Arrays.asList( title, track, disc, gap1,
            artist, album, album_artist, year, genre, composer, publisher, gap2,
            rating, playcount, comment, category, gap3,
            filesize, filename, format, bitrate, encoding, location);
        
        layout.addChild(tiles);
        Util.setAnchors(tiles, 3);
        
        
        // bind rating to app configs
        rater.icons.bind(App.maxRating);
        rater.partialRating.bind(App.partialRating);
        rater.updateOnHover.bind(App.hoverRating);
        rater.editable.bind(App.allowRatingChange);
        // write metadata on rating change
        rater.setOnRatingChanged( r -> MetadataWriter.useToRate(data, r));
        
        // accept audio drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle audio drag transfers
        entireArea.setOnDragDropped( e -> {
            if(DragUtil.hasAudio(e.getDragboard())) {
                // get first item
                List<Item> items = DragUtil.getAudioItems(e);
                // getMetadata, refresh
                if (!items.isEmpty()) setValue(items.get(0).getMetadata());
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        // if image drag detected open activity pane
        entireArea.addEventFilter(DRAG_ENTERED,e->{
            if (hasImage(e.getDragboard()) && e.getGestureSource()!=cover.getPane())
                getArea().setActivityVisible(true);
        });
        entireArea.addEventFilter(DRAG_OVER,e->{
            if (hasImage(e.getDragboard()) && e.getGestureSource()!=cover.getPane())
                getArea().setActivityVisible(true);
        });
        
        
        actPane = new ActionChooser();
        actPane.addEventHandler(DRAG_EXITED, e-> getArea().setActivityVisible(false));
        actPane.addEventHandler(MOUSE_EXITED, e-> getArea().setActivityVisible(false));
        
        
        Icon coverB = actPane.addIcon(PLUS_SQUARE, "Set as cover", true,true);
        coverB.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        coverB.setOnDragDropped( e -> {
            if(data!=null && data.isFileBased()) {  //&& DragUtil.hasImage(e.getDragboard())
                 new Fut<>()
                    .then(DragUtil.getImage(e))
                    .use(f -> copyFileSafe(f, data.getLocation(), "cover"))
                    .thenR(() -> {
                        cover_source.applyValue();              // refresh cover
                        getArea().setActivityVisible(false);    // hide activity
                    },FX)
                    .showProgress(App.getWindow().taskAdd())
                    .run();
                 
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        coverB.setOnMouseClicked(e -> {
            File f = actPane.item;
            if(f !=null && ImageFileFormat.isSupported(f)) {
                new Fut<>()
                    .thenR(() -> copyFileSafe(f, data.getLocation(), "cover"))
                    .thenR(() -> {
                        cover_source.applyValue();              // refresh cover
                        getArea().setActivityVisible(false);    // hide activity
                    },FX)
                    .showProgress(App.getWindow().taskAdd())
                    .run();
            }
        });
        Icon copyB = actPane.addIcon(PLUS, "Copy to the location", true,true);
        copyB.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        copyB.setOnDragDropped( e -> {
            if(data!=null && data.isFileBased()) {
                 new Fut<>()
                    .then(DragUtil.getImages(e))
                    .then(imgs -> copyFiles(imgs, data.getLocation()))
                    .thenR(() -> {
                        cover_source.applyValue();              // refresh cover
                        getArea().setActivityVisible(false);    // hide activity
                    },FX)
                    .showProgress(App.getWindow().taskAdd())
                    .run();
                
                e.setDropCompleted(true);
                e.consume();
            }
        });
        srcB = actPane.addIcon(SQUARE_ALT, "Source");
        srcB.setOnMouseClicked(e -> {
            if(e.getButton()==PRIMARY) readMode.setNextNapplyValue();
            if(e.getButton()==SECONDARY) readMode.setPreviousNapplyValue();
            ReadMode r = readMode.getValue();
            GlyphIconName i = r==PLAYING ? PLAY : ANDROID;
            srcB.setText(i.characterToString());
            srcB.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;",i.getFontFamily(), 15));
            srcB.getStyleClass().add("glyph");
            actPane.description.setText("left click: " + readMode.next().toString() + "\n" +
                                        "right click: " + readMode.previous().toString());
        });
        srcB.setOnMouseEntered(e->{
            actPane.description.setText("left click: " + readMode.next().toString() + "\n" +
                                        "right click: " + readMode.previous().toString());
        });
    }
    
    ActionChooser<File> actPane;
    Icon srcB;
    
    @Override
    public Node getActivityNode() {
        return actPane;
    }
    
    @Override
    public void close() {
        if (dataMonitoring != null) dataMonitoring.unsubscribe();
    }
    
/********************************* PUBLIC API *********************************/
 
    @Override
    public void refresh() {
        // apply configurables
        readMode.applyValue();
        minColumnWidth.applyValue();
        cover_source.applyValue();
        overrun_style.applyValue();
        showCover.applyValue();
        showFields.applyValue();
    }
    
    @Override
    public boolean isEmpty() {
        return data == null;
    }

    @Override
    public void setValue(Metadata m) {
        // prevent refreshing location if shouldnt
        if(!allowNoContent && m==EMPTY) return;
        
        // remember data
        data = m;
        // gui (fill out data)
        if (m == null) {
            clear();
        } else {
            // set image
            setCover(cover_source.getValue());

            // set rating
            rater.rating.set(m.getRatingPercent());

            // set other fields
            title.setText("title: "               + m.getFieldS(TITLE,""));
            track.setText("track: "               + m.getFieldS(TRACK_INFO,""));
            disc.setText("disc: "                 + m.getFieldS(DISCS_INFO,""));
            gap1.setText(" ");      
            artist.setText("artist: "             + m.getFieldS(ARTIST,""));
            album.setText("album: "               + m.getFieldS(ALBUM,""));
            album_artist.setText("album artist: " + m.getFieldS(ALBUM_ARTIST,""));
            year.setText("year: "                 + m.getFieldS(YEAR,""));
            genre.setText("genre: "               + m.getFieldS(GENRE,""));
            composer.setText("composer: "         + m.getFieldS(COMPOSER,""));
            publisher.setText("publisher: "       + m.getFieldS(PUBLISHER,""));
            gap2.setText(" ");      
            rating.setText("rating: "             + m.getFieldS(RATING,""));
            playcount.setText("playcount: "       + m.getFieldS(PLAYCOUNT,""));
            comment.setText("comment: "           + "");
            category.setText("category: "         + m.getFieldS(CATEGORY,""));
            gap3.setText(" ");      
            filesize.setText("filesize: "         + m.getFieldS(FILESIZE,""));
            filename.setText("filename: "         + m.getFieldS(FILENAME,""));
            format.setText("format: "             + m.getFieldS(FORMAT,""));
            bitrate.setText("bitrate: "           + m.getFieldS(BITRATE,""));
            encoding.setText("encoding: "         + m.getFieldS(ENCODING,""));
            location.setText("location: "         + m.getFieldS(PATH,""));
        } 
        
        // manually applied c.v.
        setVisibility();
    }
    
    private void clear() {
        rater.rating.set(0d);
        title.setText("title: ");
        track.setText("track: ");
        disc.setText("disc: ");
        gap1.setText(" ");
        artist.setText("artist: ");
        album.setText("album: ");
        album_artist.setText("album artist: ");
        year.setText("year: ");
        genre.setText("genre: " );
        composer.setText("composer: ");
        publisher.setText("publisher: ");
        gap2.setText(" ");
        rating.setText("rating: ");
        playcount.setText("playcount: ");
        comment.setText("comment: " );
        category.setText("category: ");
        gap3.setText(" ");
        filesize.setText("filesize: ");
        filename.setText("filename: ");
        format.setText("format: ");
        bitrate.setText("bitrate: ");
        encoding.setText("encoding: ");
        location.setText("location: ");
    }
    
    private void setVisibility() {
        // initialize
        visible_labels.clear();
        visible_labels.addAll(labels);
        tiles.getChildren().clear();
        visible_labels.forEach(l -> l.setDisable(false));

        // disable empty fields
        if (showEmptyFields.getValue()) {
            visible_labels.stream().filter(l->{
                    // filter out nonempty
                    String content = l.getText().substring(l.getText().indexOf(": ")+2).trim();
                    return content.isEmpty() ||
                             content.equalsIgnoreCase("?/?") ||
                               content.equalsIgnoreCase("n/a") || 
                                 content.equalsIgnoreCase("unknown");
                })
                .forEach(l->l.setDisable(true));
        } else {
            labels.stream()
                .filter(ll -> ll.getText().substring(ll.getText().indexOf(": ")+1).equals(" "))
                .forEach(visible_labels::remove);
        }
        // never disable rating, we want to be able to set the value
        rating.setDisable(false);
        
        // hide individual fields
        if (!showTitle.getValue())         visible_labels.remove(title);
        if (!showtrack.getValue())         visible_labels.remove(track);
        if (!showdisc.getValue())          visible_labels.remove(disc);
        if (!showartist.getValue())        visible_labels.remove(artist);
        if (!showalbum_artist.getValue())  visible_labels.remove(album_artist);
        if (!showalbum.getValue())         visible_labels.remove(album);
        if (!showyear.getValue())          visible_labels.remove(year);
        if (!showgenre.getValue())         visible_labels.remove(genre);
        if (!showcomposer.getValue())      visible_labels.remove(composer);
        if (!showpublisher.getValue())     visible_labels.remove(publisher);
        if (!showrating.getValue())        visible_labels.remove(rating);
        if (!showplaycount.getValue())     visible_labels.remove(playcount);
        if (!showcomment.getValue())       visible_labels.remove(comment);
        if (!showcategory.getValue())      visible_labels.remove(category);
        if (!showfilesize.getValue())      visible_labels.remove(filesize);
        if (!showformat.getValue())        visible_labels.remove(format);
        if (!showfilename.getValue())      visible_labels.remove(filename);
        if (!showbitrate.getValue())       visible_labels.remove(bitrate);
        if (!showencoding.getValue())      visible_labels.remove(encoding);
        if (!showlocation.getValue())      visible_labels.remove(location);
        
        // hide separators
        if (!groupFields.getValue()) {
            visible_labels.remove(gap1);
            visible_labels.remove(gap2);
            visible_labels.remove(gap3);
        }
        
        // show remaining
        tiles.getChildren().addAll(visible_labels);
        
        // fix rating value (we have to set text anyway to be able to tell, if
        // rating is empty (same way the other labels)
        // in the end we must remove the text because we use stars instead
        rating.setText("rating: ");
        
        // bugfix (work in progress)
        entireArea.applyCss();
        entireArea.autosize();
        entireArea.layout();
        entireArea.requestLayout();
        
        resize(entireArea.getWidth(), entireArea.getHeight());
    }
    
    private void setCover(CoverSource source) {
        // get image
        if (data == null) {
            layout.setImage((Image)null); // clear
        } else {
            Cover c = data.getCover(source);
            if(c.getFile()!=null) layout.setImage(c.getFile());
            else layout.setImage(c.getImage());
        }
    }
    
    private void setCoverVisible(boolean v) {
        layout.setShowImage(v);        
    }
    
    private void resize(double width, double height) {
        double cellH = 15+tiles.getVgap();
        int rows = (int)floor(max(height, 5)/cellH);
        if(rows==0) rows=1;
        int columns = 1+(int) ceil(visible_labels.size()/rows);
        double cellW = columns==1 || columns==0 
            // dont allow 0 columns & set whole width if 1 column
            // handle 1 column manually - the below caused some problems
            ? width
            // for n elements there is n-1 gaps so we need to add 1 gap width
            // above cell width includes 1 gap width per element so substract it
            : (width + tiles.getHgap())/columns - tiles.getHgap();
        
        // adhere to requested minimum size
        cellW = max(cellW, minColumnWidth.getValue());
        
        double w = cellW;
        tiles.setPrefTileWidth(w);
        visible_labels.forEach(l -> l.setMaxWidth(w));
    }
    
}