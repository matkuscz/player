
package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Chapters.MetadataExtended;
import AudioPlayer.tagging.Cover.Cover;
import AudioPlayer.tagging.Cover.Cover.CoverSource;
import AudioPlayer.tagging.Cover.FileCover;
import AudioPlayer.tagging.Cover.ImageCover;
import Configuration.Configuration;
import PseudoObjects.FormattedDuration;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.images.Artwork;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;
import utilities.Parser.ColorParser;
import utilities.Util;

/**
 * Information about audio file.
 * Provided by reading tag and audio header. Plus some additional sources.
 * Everything that is possible to know about the audio file is virtually grouped
 * in this class. Virtually, because not everything accessible through this
 * object is in the tag.
 * <p>
 * The class is immutable.
 * <p>
 * Metadata can be empty. All string fields will be empty and object fields
 * null. The {@link #getFile()}, {@link #getURI()} and {@link #getPath()} of the
 * item will not point to any existing file.
 * Empty metadata should always be used instead of null value. See {@link #EMPTY()}
 * and {@link #isEmpty()}.
 * ------------- NON TAG INFO ---------------
 * Non-tag information possible to gain from metadata object includes:
 * -- Cover --
 * getCoverFromLocation()
 * getCoverFromAnySource()
 * -- Playlist specific information --
 * getPlaylistOrder()
 * getExtended()
 * 
 * ------------- GETTERS ---------------
 * The getters of this class return mostly string. For empty fields the output
 * is "" (empty string) and for non-string getters it is null.
 * 
 * 
 * To find out more specific details, it is advised to check methods' documentation.
 * 
 * Supported fields:
 *    format
 *    bitrate;
 *    sample rate;
 *    channels;
 *    encoder;
 *    encoder type
 *    length;
 *    title;
 *    album;
 *    artist;
 *    artists;
 *    album_artist;
 *    composer;
 *    publisher;
 *    track;
 *    tracks_total;
 *    disc;
 *    discs_total;
 *    genre;
 *    year;
 *    cover;
 *    rating;
 *    playcount;
 *    category;
 *    comment;
 *    lyrics;
 *    mood;
 *    custom 1-5
 * 
 * @author uranium
 */
@Immutable
public final class Metadata extends MetaItem {
    private final URI uri;
    
    // header fields
    private String format = "";
    private String encoding_type = "";
    private Bitrate bitrate = Bitrate.create(-1);
    private String encoder = "";
    private String channels = "";
    private String sample_rate = "";
    private double length = 0;
    
    // tag fields
    private String title = "";
    private String album = "";
    private String artist = "";
    private List<String> artists = new ArrayList<>();
    private String album_artist = "";
    private String composer = "";
    private String publisher = "";
    private String track = "";
    private String tracks_total = "";
    private String disc = "";
    private String discs_total = "";
    private String genre = "";
    private String year = "";
    private Artwork cover;
    private Long rating = null;
    private Long playcount = null;
    private String category = "";
    private String comment = "";
    private String lyrics = "";
    private String mood = "";
    private String custom1 = "";
    private String custom2 = "";
    private String custom3 = "";
    private String custom4 = "";
    private String custom5 = "";
    
    /** 
     * Creates empty metadata. All string fields will be empty and object fields
     * null. The File, URI and path of the item will not point to any existing file.
     * Empty metadata should always be used instead of null value.
     */
    public static Metadata EMPTY() {
        return new Metadata();
    }
    
    /**
     * Creates metadata from specified item. Attempts to fill as many metadata 
     * information fields from the item as possible, leaving everything else empty.
     * For example, if the item is playlist item, sets artist, length and title fields.
     */
    public Metadata(Item item) {
        uri = item.getURI();
        if(item instanceof PlaylistItem) {
            PlaylistItem pitem = (PlaylistItem)item;
            artist = pitem.getArtist();
            length = pitem.getTime().toMillis();
            title = pitem.getTitle();
        }
    }
    
    private Metadata() {
        uri = new File("").toURI();
    }
    
    /**
     * Reads and creates metadata info for file.
     * Everything is read at once. There is no additional READ operation after the
     * object has been created.
     * 
     */
    Metadata(AudioFile audiofile) {
        uri = audiofile.getFile().getAbsoluteFile().toURI();
        AudioFile audioFile = audiofile;
        
        loadGeneralFields(audioFile);
        switch (getFormat()) {
            case mp3:   MP3File mp3 = (MP3File)audioFile;
                        loadSpecificFieldsMP3(mp3);
                        break;
            case flac:  //FLACFile flac = (MP3File)audioFile;
                        //loadSpecificFieldsFlac(flac);
                        break;
            case ogg:   //MP3File ogg = (MP3File)audioFile;
                        //loadSpecificFieldsOGG(ogg);
                        break;
            case wav:   //MP3File ogg = (MP3File)audioFile;
                        loadSpecificFieldsWAV();
                        break;
            default:
        }        
        
    }
    /** loads all generally supported fields  */
    private void loadGeneralFields(AudioFile aFile) {
        Tag tag = aFile.getTag();
        AudioHeader header = aFile.getAudioHeader();
        
        // format and encoding type are switched in jaudiotagger library...
        format = Util.emptifyString(header.getEncodingType());
        bitrate = Bitrate.create(header.getBitRateAsNumber());
        length = 1000 * header.getTrackLength();
        encoding_type = Util.emptifyString(header.getFormat());
        channels = Util.emptifyString(header.getChannels());
        sample_rate = Util.emptifyString(header.getSampleRate());
        
        encoder = getGeneral(tag,FieldKey.ENCODER);
        
        title = getGeneral(tag,FieldKey.TITLE);
        album = getGeneral(tag,FieldKey.ALBUM);
        artists = getGenerals(tag,FieldKey.ARTIST);
        artist = getGeneral(tag,FieldKey.ARTIST);
        album_artist = getGeneral(tag,FieldKey.ALBUM_ARTIST);
        composer = getGeneral(tag,FieldKey.COMPOSER);
        
        track = getGeneral(tag,FieldKey.TRACK);
        tracks_total = getGeneral(tag,FieldKey.TRACK_TOTAL);
        disc = getGeneral(tag,FieldKey.DISC_NO);
        discs_total = getGeneral(tag,FieldKey.DISC_TOTAL);
        genre = getGeneral(tag,FieldKey.GENRE);
        year = getGeneral(tag,FieldKey.YEAR);
        
        category = getGeneral(tag,FieldKey.GROUPING);
        cover = tag.getFirstArtwork();
        comment = getComment(tag);
        lyrics = getGeneral(tag,FieldKey.LYRICS);
        mood = getGeneral(tag,FieldKey.MOOD);
        custom1 = getGeneral(tag,FieldKey.CUSTOM1);
        custom2 = getGeneral(tag,FieldKey.CUSTOM2);
        custom3 = getGeneral(tag,FieldKey.CUSTOM3);
        custom4 = getGeneral(tag,FieldKey.CUSTOM4);
        custom5 = getGeneral(tag,FieldKey.CUSTOM5);
    }
    private String getGeneral(Tag tag, FieldKey f) {
        if (!tag.hasField(f)) return "";
        return Util.emptifyString(tag.getFirst(f));
    }
    // use this to get comment, not getField(COMMENT); because it is bugged
    private String getComment(Tag tag) { 
// there is a bug where getField(Comment) returns CUSTOM1 field, this is workaround
// this is how COMMENT field look like:
//      Language="English"; Text="bnm"; 
// this is how CuSTOM fields look like:  
//      Language="Media Monkey Format"; Description="Songs-DB_Custom5"; Text="mmmm"; 
        if (!tag.hasField(FieldKey.COMMENT)) return "";
        int i = -1;
        List<TagField> fields = tag.getFields(FieldKey.COMMENT);
        // get index of comment within all comment-type tags
        for(TagField t: fields) // custom
            if (!t.toString().contains("Description"))
                i = fields.indexOf(t);
        if(i>-1) return tag.getValue(FieldKey.COMMENT, i);
        else return "";
    }
    
    private List<String> getGenerals(Tag tag, FieldKey f) {
        List<String> out = new ArrayList<>(); 
        if (!tag.hasField(f)) return out;
        
        try{
//        for (TagField field: tag.getFields(f)) {
//            String tmp = field.toString();
//                   tmp = tmp.substring(6, tmp.length()-3);
//            out.add(Util.emptifyString(tmp));
//        }
        for (String val: tag.getAll(f))
            out.add(Util.emptifyString(val));
        
        
        }catch(Exception w) { // no its not broad exception, some other shit keeps happening too
            w.printStackTrace();
        }
        return out;
    }
    
    private void loadSpecificFieldsMP3(MP3File mp3) {
        AbstractID3v2Frame frame1 = mp3.getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
        FrameBodyPOPM body1 = null;
        Long rat = null;
        Long cou = null;
        if (frame1 != null) {
            body1 = (FrameBodyPOPM) frame1.getBody();
        }
        if (body1 != null) {
            rat = body1.getRating();
            cou = body1.getCounter();
        }
        
        rating = rat; //returns null if empty
        playcount = cou; // returns null if empty
        publisher = Util.emptifyString(mp3.getID3v2TagAsv24().getFirst(ID3v24Frames.FRAME_ID_PUBLISHER));
    }
    private void loadSpecificFieldsWAV() {
        rating = null;
        playcount = null;
        publisher = "";
    }
    private void loadSpecificFieldsOGG() {
        rating = null;
        playcount = null;
        publisher = "";
    }
    private void loadSpecificFieldsFLAC() {
        rating = null;
        playcount = null;
        publisher = "";
    }

/******************************************************************************/  

    @Override 
    public URI getURI() {
        return uri;
    }
    
    /** 
     * Empty metadata should always be used instead of null value.
     * @{@inheritDoc}
     * @return true if this metadata is empty.
     */
    public boolean isEmpty() {
        return new File("").toURI().equals(getURI());
    }
    
    // because of the above we need to override the following two methods:
    
    /** @{@inheritDoc} */
    @Override
    public String getPath() {
        if (isEmpty()) return "";
        else return super.getPath();
    }
    
    /** @{@inheritDoc} */
    @Override
    public FileSize getFilesize() {
        if (isEmpty()) return new FileSize(0);
        return super.getFilesize();
    }
    
    
    
    
    /**
     * 
     * String returning alternative to {@link #getSuffix} and {@link #getFormat}.
     * These methods are not equivalent as this method returns information from tag.
     * <p>
     * For example: mp3.
     * @return format
     */
    public String getFormatFromTag() {
        return format;
    }

    /** @return the bitrate */
    public Bitrate getBitrate() {
        return bitrate;
    }
    
    /**
     * For example: Stereo.
     * @return 
     */
    public String getChannels() {
        return channels;
    }
    
    /**
     * For example: 44100
     * @return 
     */
    public String getSampleRate() {
        return sample_rate;
    }
    
    /**
     * For example: MPEG-1 Layer 3.
     * @return encoding type
     */
    public String getEncodingType() {
        // format and encoding type are switched in jaudiotagger library...
        return encoding_type;
    }
    
    /** @return the encoder or empty String if not available. */
    public String getEncoder() {
        return encoder;
    }
    
    /** @return the length in milliseconds */
    public FormattedDuration getLength() {
        return new FormattedDuration(length);
    }
    
/******************************************************************************/    
    
    /**
     * @return the title
     * "" if empty.
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * @return the album
     * "" if empty.
     */
    public String getAlbum() {
        return album;
    }
    
    /**
     * Returns list all of artists.
     * @return the artist
     * empty List if empty.
     */
    public List<String> getArtists() {
        return artists;
    }
    
    /**
     * Returns all of artists as a String. Uses ", " separator. One line
     * Empty string no arist.
     * @return 
     */
    public String getArtistsAsString() {
        String temp = "";
        for (String s: artists) {
            temp = temp + ", " + s;
        }
        return temp;
    }
    
    /**
     * Returns first artist found in tag.
     * If you want to get all artists dont use this method.
     * @return the first artist
     * "" if empty.
     */    
    public String getArtist() {
        return artist;
    }
    
    /**
     * Returns first artist found in tag. If no artist is found, album artist
     * will be used, but only if this option is enabled in application configurations.
     * If 'album artist when no artist' is not enabled, this method is effectively
     * same as getArtist().
     * If you want to get all artists dont use this method.
     * @return the first artist or album artist
     * "" if both sources are empty.
     */    
    public String getArtistOrAlbumArist() {
        if (!artist.isEmpty()) return artist;
        if (Configuration.ALBUM_ARTIST_WHEN_NO_ARTIST) return album_artist;
        return "";
    }
    
    /**
     * @return the album_artist
     * "" if empty.
     */
    public String getAlbumArtist() {
        return album_artist;
    }

    /**
     * @return the composer
     * "" if empty.
     */
    public String getComposer() {
        return composer;
    }

    /**
     * @return the publisher
     * "" if empty.
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * @return the track
     * "" if empty.
     */
    public String getTrack() {
        return track;
    }
    
    /**
     * @return the tracks total
     * "" if empty.
     */
    public String getTracksTotal() {
        return tracks_total;
    }
    
    /**
     * Convenience method. Returns complete info about track order within album in
     * format track/tracks_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getTrack()} and {@link #getTracksTotal())}.
     * <p>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, 4/23, ?/?, ...
     * @return track album order information.
     */
    public String getTrackInfo() {
        if (!track.isEmpty() && !tracks_total.isEmpty()) {
            return track+"/"+tracks_total;
        } else
        if (!track.isEmpty() && tracks_total.isEmpty()) {
            return track+"/?";
        } else        
        if (track.isEmpty() && !tracks_total.isEmpty()) {
            return "?/"+tracks_total;
        } else {
        //if (track.isEmpty() && tracks_total.isEmpty()) {
            return "?/?";
        }
    }
    
    /**
     * @return the disc
     * "" if empty.
     */
    public String getDisc() {
        return disc;
    }
    
    /**
     * @return the discs total
     * "" if empty.
     */
    public String getDiscsTotal() {
        return discs_total;
    }
    
    /**
     * Convenience method. Returns complete info about disc number in album in
     * format disc/discs_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getDisc() ()} and {@link #getDiscsTotal()}.
     * <p>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, ?/?, 5/?, ...
     * @return disc information.
     */
    public String getDiscInfo() {
        if (!disc.isEmpty() && !discs_total.isEmpty()) {
            return disc+"/"+discs_total;
        } else
        if (!disc.isEmpty() && discs_total.isEmpty()) {
            return disc+"/?";
        } else        
        if (disc.isEmpty() && !discs_total.isEmpty()) {
            return "?/"+discs_total;
        } else {
        //if (disc.isEmpty() && discs_total.isEmpty()) {
            return "?/?";
        }
    }
    
    /**
     * @return the genre
     */
    public String getGenre() {
        return genre;
    }

    /**  @return the year. "" if empty. */
    public String getYear() {
        return year;
    }

    /**
     * Do not use. Will be removed.
     * @return the cover image. Null if doesn't exist */
    @Deprecated
    public Artwork getCoverAsArtwork() {
        return cover;
    }
    
    /**
     * Returns cover from the respective source.
     * @param source
     */
    public Cover getCover(CoverSource source) {
        switch(source) {
            case TAG: return getCover();
            case DIRECTORY: return new FileCover(getCoverFromDirAsFile(), "");
            case ANY : {
                Cover c = getCover();
                return c.getImage()!=null 
                        ? c 
                        : new FileCover(getCoverFromDirAsFile(), "");
            }
            default: throw new AssertionError(source + " in default switch value.");
        }
    }
    
    private Cover getCover() {
        try {
            if(cover==null) return new ImageCover((Image)null, getCoverInfo());
            else return new ImageCover((BufferedImage) cover.getImage(), getCoverInfo());
        } catch (IOException ex) {
            return new ImageCover((Image)null, getCoverInfo());
        }
    }
    
    private String getCoverInfo() {
        try {
            return cover.getDescription() + " " + cover.getMimeType() + " "
                       + ((RenderedImage)cover.getImage()).getWidth() + "x"
                       + ((RenderedImage)cover.getImage()).getHeight();
        } catch(IOException | NullPointerException e) {
            // nevermind errors. Return "" on fail.
            return "";
        }
    }
    
    /**
     * Identical to getCoverFromDir() method, but returns the image file
     * itself. Only file based items.
     * <p>
     * If the Image object of the cover suffices, it is recommended to
     * avoid this method, or even better use getCoverFromAnySource()
     * @return 
     */
    private File getCoverFromDirAsFile() {
        if(!isFileBased()) return null;
        
        File dir = getFile().getParentFile();
        if (!FileUtil.isValidDirectory(dir)) return null;
                
        File[] files;
        files = dir.listFiles( f -> {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if(i == -1) return false; 
            String name = filename.substring(0, i);
            return (ImageFileFormat.isSupported(f.toURI()) && (
                        name.equalsIgnoreCase("cover") ||
                        name.equalsIgnoreCase("folder") ||
                        (isFileBased() && (
                            name.equalsIgnoreCase(getFilenameFull()) ||
                            name.equalsIgnoreCase(getFilename()))) ||
                        name.equalsIgnoreCase(getTitle()) ||
                        name.equalsIgnoreCase(getAlbum())
                ));
        });
        
        if (files.length == 0) return null;
        else return files[0];
    }
    
    /** @return the rating null if empty. */
    public Long getRating() {
        return rating;
    }
    
    /** @return the rating in 0-1 percent range */
    public double getRatingPercent() {
        return rating/getRatingMax();
    }
    
    /** @return the rating "" if empty. */
    public String getRatingAsString() {
        if (rating == null)  return ""; 
        return String.valueOf(rating.intValue());
    }
    
    /** @return the rating in 0-1 percent range or "" if empty. */
    public String getRatingPercentAsString() {
        if (rating == null)  return ""; 
        return String.valueOf(getRatingPercent());
    }
    
    /**
     * @param max_stars
     * @return the current rating value in 0-$max_stars value system. 0 if not available.
     */
    public double getRatingToStars(int max_stars) {
        if (getRating()==null) return 0;
        return getRatingPercent()*max_stars;
    }
    
    /**
     * @return the playcount
     * 0 if empty.
     */
    public int getPlaycount() {
        if (playcount == null) return 0;
        else return playcount.intValue();
    }
    
    /**
     * @return the playcount
     * "" if empty.
     */
    public String getPlaycountAsString() {
        return (playcount == null) ? "" : String.valueOf(playcount.intValue());
    }
    
    /** 
     * tag field: grouping
     * @return the category
     * "" if empty.
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * @return the comment
     * "" if empty.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the lyrics or "" if empty.
     */
    public String getLyrics() {
        return lyrics;
    }

    /**  @return the mood or "" if empty. */
    public String getMood() {
        return mood;
    }
    
    /**  
     * Color is located in the Custom1 tag field.
     * @return the color value associated with the song from tag or null if
     * none.
     */
    public Color getColor() {
         return custom1.isEmpty() ? null : new ColorParser().fromS(custom1);
    }
    
    /** @return the value of custom1 field. "" if empty. */
    public String getCustom1() {
        return custom1;
    }
    /** @return the value of custom2 field. "" if empty. */
    public String getCustom2() {
        return custom2;
    }
    /** @return the value of custom3 field. "" if empty. */
    public String getCustom3() {
        return custom3;
    }
    /** @return the value of custom4 field. "" if empty. */
    public String getCustom4() {
        return custom4;
    }
    /** @return the value of custom5 field. "" if empty. */
    public String getCustom5() {
        return custom5;
    }
    
/******************************************************************************/
    
    /**
     * Note: This is non-tag information.
     * @return some additional non-tag information
     */
    public MetadataExtended getExtended() {
        MetadataExtended me = new MetadataExtended(this);
                         me.readFromFile();
        return me;
    }
    
    /**
     * @return index of the item in playlist belonging to this metadata or -1 if
     * not on playlist.
     */
    public int getPlaylistIndex() {
        return PlaylistManager.getIndexOf(this)+1;
    }
    
    /**
     * Returns index of the item in playlist belonging to this metadata.
     * Convenience method. Example: "15/30". "Order/total" items in playlist.
     * Note: This is non-tag information.
     * @return Empty string if not on playlist. Never null.
     */
    public String getPlaylistIndexInfo() {
        int i = getPlaylistIndex();
        return i==-1 ? "" : i + "/" + PlaylistManager.getSize();
    }
    
    /**
     * Returns chapters associated with this item. A {@link Chapter} is a textual
     * information associated with time and the song item. There are usually
     * many chapters per item.
     * <p>
     * Chapters string is located in the Custom2 tag field.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from tag data
     */
    public List<Chapter> getChapters() {
        List<Chapter> cs = new ArrayList();
        // we have got a regex over here so "||\" is equivalent to '\' character
        for(String c: getCustom2().split("\\|", 0)) {
            try {
                cs.add(new Chapter(c));
            } catch( IllegalArgumentException e) {
                Log.err("String '" + c + "' not parsable chapter string. Will be ignored.");
                // ignore
            }
        }
        return cs;
    }
    
    /**
     * Legacy method.
     * Before chapters were written to tag they were contained within xml files
     * in the same directory as the item and the same file name (but xml suffix).
     * This method reads the file when it is invoked and parses its content.
     * <p>
     * This method should only be used when transferring information in legacy
     * xml files into tag.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from xml data
     */
    public List<Chapter> getChaptersFromXML() {
        MetadataExtended me = new MetadataExtended(this);
                         me.readFromFile();
        List<Chapter> cs = me.getChapters();
                      cs.sort(Chapter::compareTo);
        return cs;
    }
    
    /**
     * Convenience method combining the results of {@link #getChapters()} and 
     * {@link #getChaptersFromXML()}.
     * <p>
     * This method should only be used when transferring information in legacy
     * xml files into tag.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from all available sources
     */
    public List<Chapter> getChaptersFromAny() {
        List<Chapter> cs = getChapters();
                      cs.addAll(getChaptersFromXML());
                      cs.sort(Chapter::compareTo);
        return cs;
    }
    
/******************************************************************************/
    
    /** 
     * Non-tag data and chapters are excluded.
     * @return mostly complete information about item's metadata in string form. 
     */
    @Override
    public String toString() {
        String output;
        
        output = "path:  " + getPath() + "\n"
               + "name: " + getFilename() + "\n"
               + "format: " + getFormat() + "\n"
               + "filesize: " + getFilesize().toString() + "\n"

               + "bitrate: " + bitrate + "\n"
               + "encoder: " + encoder + "\n"
               + "length: " + getLength().toString() + "\n"

               + "title: " + title + "\n"
               + "album: " + album + "\n"
               + "artists: " + artists.size() + "\n"
               +  artists.stream().map(a->"    " + a + "\n").collect(Collectors.joining(""))
               + "album artist: " + album_artist + "\n"
               + "composer: " + composer + "\n"
               + "publisher: " + publisher + "\n"

               + "track: " + getTrackInfo() + "\n"
               + "disc: " + getDiscInfo() + "\n"
               + "genre: " + genre + "\n"
               + "year: " + year + "\n"

               + "cover: " + getCoverInfo() + "\n"

               + "rating: " + getRatingAsString() + "\n"
               + "playcount: " + getPlaycountAsString() + "\n"
               + "category: " + category + "\n"
               + "comment: " + comment + "\n"
               + "lyrics: " + lyrics + "\n"
               + "mood: " + mood + "\n"
               + "custom1: " + custom1 + "\n"
               + "custom2: " + custom2 + "\n"
               + "custom3: " + custom3 + "\n"
               + "custom4: " + custom4 + "\n"
               + "custom5: " + custom5 + "\n";
        
        return output;
    }
    
}