
package AudioPlayer.tagging;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Notifier.Notifier;
import AudioPlayer.tagging.Chapters.Chapter;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.max;
import java.net.URI;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import static javafx.application.Platform.runLater;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.paint.Color;
import main.App;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import static org.jaudiotagger.tag.FieldKey.CUSTOM3;
import static org.jaudiotagger.tag.FieldKey.RATING;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPUB;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import util.File.AudioFileFormat;
import static util.File.AudioFileFormat.*;
import static util.Util.clip;
import static util.async.Async.runNew;
import util.dev.Log;
import util.dev.TODO;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import util.parsing.Parser;

/**
 * 
 * Manages writing Metadata objects back into files. Handles all tag related data
 * for items.
 * <p>
 * The writer must be instantiated for use. It is reusable for an item.
 *  
 * @author uranium
 */
@TODO(purpose = FUNCTIONALITY, note = "limit rating bounds value, multiple values, id3 popularimeter mail settings")
public class MetadataWriter extends MetaItem {
    
    // state
    private File file;
    private AudioFile audioFile;
    private Tag tag;
    private int fields_changed = 0;
    // properties
    private final ReadOnlyBooleanWrapper isWriting = new ReadOnlyBooleanWrapper(false);
    public final ReadOnlyBooleanProperty writing = isWriting.getReadOnlyProperty();

    // dont provide access here
    public MetadataWriter(){}
    private MetadataWriter(File file, AudioFile audioFile) {
        this.file = file;
        this.audioFile = audioFile;
    }
    
    /** 
     * Constructs metadata writer for given item.
     * @return writer or null if error occurs.
     * @throws UnsupportedOperationException if item not file based
     */
    public static MetadataWriter create(Item item) {
        if (!item.isFileBased()) throw new UnsupportedOperationException("Item must be file based");
        
        MetadataWriter w = new MetadataWriter();
                       w.reset(item);
        return w.audioFile==null ? null : w;
    }
    

    
    @Override
    public URI getURI() {
        if(file==null) throw new IllegalStateException("Illegal getUri call. metadata writer state not initialized.");
        return file.toURI();
    }
    @Override
    public File getFile() {
        return file;
    }

    /**
     * Changes name of the file of the item.
     * @param filename without extension
     */
    public void changeFilename(String filename) {
//        this.filename = filename;
    }

    /** @param encoder the encoder to set */
    public void setEncoder(String encoder) {
        setField(FieldKey.ENCODER, encoder);
    }

    /** @param album the album to set */
    public void setAlbum(String album) {
        setField(FieldKey.ALBUM, album);
    }

    /**  @param val the artist to set */
    public void setArtist(String val) {
        setField(FieldKey.ARTIST, val);
    }
    
    /**  @param val the album_artist to set */
    public void setAlbum_artist(String val) {
        setField(FieldKey.ALBUM_ARTIST, val);
    }
    
    /**  @param artists the artists to set */
    public void setArtists(List<String> artists) {
        if (artists == null || artists.isEmpty())
            setArtist(null);
        else
            artists.stream().filter(String::isEmpty).forEach(a->{
                try {
                    tag.createField(FieldKey.ARTIST, a);
                    fields_changed++;
                } catch (KeyNotFoundException ex) {
                Log.info("Artist field not found.");
                } catch (FieldDataInvalidException ex) {
                    Log.info("Invalid artist field data.");
                }
            });
    }
    
    /** @param val the composer to set */
    public void setComposer(String val) {
        setField(FieldKey.COMPOSER, val);
    }
    
    /**  @param val the category to set  */
    public void setCategory(String val) {
        setField(FieldKey.GROUPING, val);
    }

    /** @param val the comment to set */
    public void setComment(String val) {
        setField(FieldKey.COMMENT, val);
    }
    
    /** @param cover the cover to set */
    public void setCover(File cover) {
        try {
            if (cover == null)
                tag.deleteArtworkField();
            else {
                tag.deleteArtworkField();
                tag.setField(ArtworkFactory.createArtworkFromFile(cover));
            }
            fields_changed++;
        } catch (KeyNotFoundException ex) {
            Log.info("Category field not found.");
        } catch (FieldDataInvalidException ex) {
            Log.info("Invalid category field data.");
        } catch (IOException ex) {
            Log.info("Problem with the file reading when setting cover to tag.");
        }
    }
    
    /** @param disc the disc to set */
    public void setDisc(String disc) {
        setField(FieldKey.DISC_NO, disc);
    }

    /**  @param discs_total the discs_total to set */
    public void setDiscs_total(String discs_total) {
        setField(FieldKey.DISC_TOTAL, discs_total);
    }

    /** @param genre the genre to set */
    public void setGenre(String genre) {
        setField(FieldKey.GENRE, genre);
    }
    
    /**  
     * Sets rating to specified value. Valid number String will be parsed
     * otherwise this method is no-op. Rating value will be clipped to range
     * supported by specific tag. Note that not all tags have the same maximum
     * rating value. Because of this, it is recommended to avoid this method and
     * use percentage alternative.
     * @param rating rating value to set. Empty or null to remove the field from tag.
     * Negative value will result in no-op.
     */
    public void setRating(String rating) {
        if (rating == null || rating.isEmpty())
            setRating(-1);
        else {
            try {
                double val = Double.parseDouble(rating);
                if (val < 0)
                    throw new IllegalArgumentException("Rating number must not be negative");
                setRating(val);
            } catch (NumberFormatException ex) {
                Log.info("Rating field value not a number");
            }
        }
    }
    
    /**
     * Sets rating to specified value expressed in percentage <0-1>. Valid number
     * String will be parsed otherwise this method is no-op.
     * @param rating rating value to set. Empty or null to remove the field from tag.
     * Param not in <0-1> range will result in no-op.
     */
    public void setRatingPercent(String rating) {
        if (rating == null || rating.isEmpty())
            setRating(-1);
        else {
            try {
                double val = Double.parseDouble(rating);
                setRatingPercent(val);
            } catch (NumberFormatException ex) {
                Log.info("Rating field value not a number");
            }
        }
    }
    
    /** 
     * Sets rating to specified value expressed in percentage <0-1>. It is recommended
     * to use this method to avoid value corruption by clipping it.
     * @param val rating to set in percentage <0-1>.
     * Param not in <0-1> range will result in no-op.
     */
    public void setRatingPercent(double val){
        if (val == -1) setRating(val);
        else if (val > 1)
            Log.err("Rating number must be <= 1");
        else if (val < 0)
            Log.err("Rating number must be >= 0");
        else setRating(getRatingMax() * val);
    }
    
    /** 
     * Sets rating to specified value. Rating value will be clipped to range
     * supported by specific tag. Note that not all tags have the same maximum
     * rating value. Because of this, it is recommended to avoid this method and
     * use percentage alternative.
     * @param val rating to set. -1 to remove the field from tag. Value will be 
     * clipped to 0-max value. */
    public void setRating(double val) {
        val = val==-1 ? -1 : clipRating(val);
        
        AudioFileFormat f = getFormat();
        switch(f) {
            case mp3:   setRatingMP3(val); break;
            case flac:  Log.info("Unsupported operation."); break;
            case ogg:   Log.info("Unsupported operation."); break;
            case wav:   Log.info("Unsupported operation."); break;
            case mp4:
            case m4a:   setRatingMP4(val); break;
            default: throw new AssertionError("corrupted switch statement");
        } 
    }
    
    private void setRatingMP3(double val) {
        MP3File mp3File = ((MP3File)audioFile);
                mp3File.getTagOrCreateAndSetDefault();
        AbstractID3v2Frame f = mp3File.getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
        if ( f == null) { 
             f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
             f.setBody(new FrameBodyPOPM());
        }
        try {
            if (val == -1) {
                ((FrameBodyPOPM) f.getBody()).setRating(0);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            } else {
                ((FrameBodyPOPM) f.getBody()).setRating((long)val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring rating field. Data invalid.");
        }
    }
    private void setRatingMP4(double val) {
        try {
            if (val == -1) {
                tag.deleteField(RATING);
            } else {
                int r = clip(0, (int) val, 100);
                tag.setField(RATING, Integer.toString(r));
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring rating field. Data invalid.");
        }
    }

    /** @param title the title to set  */
    public void setTitle(String title) {
        setField(FieldKey.TITLE, title);
    }
    
    /** @param track the track to set */
    public void setTrack(String track) {
        setField(FieldKey.TRACK, track);
    }

    /** @param tracks_total the tracks_total to set  */
    public void setTracks_total(String tracks_total) {
        setField(FieldKey.TRACK_TOTAL, tracks_total);
    }
    
    /**  @param count the rating to set */
    public void setPlaycount(String count) {
        if (count == null || count.isEmpty())
            setPlaycount(-1);
        else {
            try {
                int val = Integer.parseInt(count);
                if (val < 0) 
                    throw new NumberFormatException("Playcount number must not be negative");
                setPlaycount(val);
            } catch (NumberFormatException ex) {
                Log.info("Playcount field value not a number");
            }
        }
    }
    
    /** @param val rating to set. -1 to remove the field from tag. */
    public void setPlaycount(int val) {
        AudioFileFormat f = getFormat();
        switch(f) {
            case ogg:
            case wav:
            case mp4:
            case m4a:
            case flac:  break;
            case mp3:   setPlaycountMP3(val); break;
            default: throw new AssertionError("corrupted switch statement");
        } 
        setField(CUSTOM3, val<0 ? "" : String.valueOf(val));
    }
    
    /** @param increments playcount by 1. */
    public void inrPlaycount(Metadata m) {
        setPlaycount(m.getPlaycount()+1);
    }
    
    private void setPlaycountMP3(int val) {
        // POPM COUNT
        try {
            // get tag
            AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
            if ( f == null) { 
                 f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
                 f.setBody(new FrameBodyPOPM()); 
            }
            // set value
            ((FrameBodyPOPM) f.getBody()).setCounter(max(0,val));
            ((MP3File)audioFile).getID3v2Tag().setField(f);
            // fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring playcount field. Data invalid.");
        }
        // PLAY COUNT
        try {
            // get tag
            AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_PLAY_COUNTER);
            if ( f == null) { 
                 f = new ID3v24Frame(ID3v24Frames.FRAME_ID_PLAY_COUNTER); 
                 f.setBody(new FrameBodyPCNT()); 
            }
            // set value
            ((FrameBodyPCNT) f.getBody()).setCounter((max(0,val)));
            ((MP3File)audioFile).getID3v2Tag().setField(f);
            // fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring playcount field. Data invalid.");
        }
    }
 

    /** @param val the publisher to set  */
    public void setPublisher(String val) {
        AudioFileFormat f = getFormat();
        switch(f) {
            case mp3:   setPublisherMP3(val); break;
            case flac:  Log.info("Unsupported operation."); break;
            case ogg:   Log.info("Unsupported operation."); break;
            case wav:   Log.info("Unsupported operation."); break;
            case m4a:   setPublisherMP4(val); break;
            default: throw new AssertionError("corrupted switch statement");
        }        
    }
    
    private void setPublisherMP3(String val) {
        AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_PUBLISHER);
        if ( f == null) { 
             f = new ID3v24Frame(ID3v24Frames.FRAME_ID_PUBLISHER); 
             f.setBody(new FrameBodyTPUB());
        }
        // set value, prevent writing corrupt data
        try {
            if (val == null || val.isEmpty())
                ((MP3File)audioFile).getID3v2Tag().removeFrameOfType(f.getIdentifier());
            else {
                ((FrameBodyTPUB) f.getBody()).setText(val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring publisher field. Data invalid.");
        }
    }
    private void setPublisherMP4(String val) {
        try {
            if (val == null || val.isEmpty()) {
                ((Mp4Tag)tag).deleteField(Mp4FieldKey.WINAMP_PUBLISHER);
                ((Mp4Tag)tag).deleteField(Mp4FieldKey.MM_PUBLISHER);
            } else {
                ((Mp4Tag)tag).setField(Mp4FieldKey.WINAMP_PUBLISHER, val);
                ((Mp4Tag)tag).setField(Mp4FieldKey.MM_PUBLISHER, val);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring publisher field. Data invalid.");
        }
    }
    
    /**
     * Change user/mail within POPM3 field of id3 tag. Supports only files
     * supporting id3 tag (mp3). For other types (flac, ogg, wav) does nothing.
     */
    public void setUserMailID3(String val) {
        
        AudioFileFormat f = getFormat();
        switch(f) {
            case mp3:   seUserPopmID3(val); break;
            case flac:  Log.info("Unsupported operation."); break;
            case ogg:   Log.info("Unsupported operation."); break;
            case wav:   Log.info("Unsupported operation."); break;
            case mp4:
            case m4a:   Log.info("Unsupported operation."); break;
            default: throw new AssertionError("corrupted switch statement");
        }
    }
    private void seUserPopmID3(String val) {
        try {
            AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
            // prevent null
            if ( f == null) { 
                 f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
                 f.setBody(new FrameBodyPOPM()); }
            // set value
            if (val == null || val.isEmpty()) {
                ((FrameBodyPOPM) f.getBody()).setEmailToUser("");
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            } else {
                ((FrameBodyPOPM) f.getBody()).setEmailToUser(val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.info("Ignoring playcount field. Data invalid.");
        }
    }

    /** @param val the lyrics to set */
    public void setLyrics(String val) {
        setField(FieldKey.LYRICS, val);
    }

    /** @param val the mood to set */
    public void setMood(String val) {
        setField(FieldKey.MOOD, val);
        MoodManager.moods.add(val);
    }
    
    /** @param c the color to set */
    public void setColor(Color c) {
        setField(FieldKey.CUSTOM1, Parser.toS(c));
    }
    
    /**
     * Write chapters to tag. This method rewrites any previous chapter data.
     * In order to not lose the original data, the chapters first need to be
     * obtained and the modified list passed as an argument to this method.
     * 
     * @param list of chapters that to write to tag
     * @see addChapter(AudioPlayer.tagging.Chapters.Chapter, AudioPlayer.tagging.Metadata)
     * @see removeChapter(AudioPlayer.tagging.Chapters.Chapter, AudioPlayer.tagging.Metadata)
     */
    public void setChapters(List<Chapter> chapters) {
        setCustom2(chapters.stream().map(Chapter::toString).collect(Collectors.joining("|")));
    }
    
    /**
     * Convenience method. 
     * Adds the given chapter to the metadata or rewrites it if it already exists.
     * For chapter identity consult {@link Chapter#equals(java.lang.Object)}.
     * <p>
     * Note: Dont abuse this method in loops and use {@link #setChapters(java.util.List)}.
     * 
     * @param chapter
     * @param metadata Source metadata for chapter data. In order to retain rest
     * of the chapters, the metadata for the item are necessary.
     */
    public void addChapter(Chapter chapter, Metadata metadata) {
        List<Chapter> chaps = metadata.getChapters();
        int i = chaps.indexOf(chapter);
        if(i==-1) chaps.add(chapter);
        else chaps.set(i, chapter);
        setChapters(chaps);
    }
    
    /**
     * Convenience method. 
     * Removes the given chapter from the metadata if it already exists or does
     * nothing otherwise.
     * <p>
     * For chapter identity consult {@link Chapter#equals(java.lang.Object)}. 
     * Dont abuse this method in loops and use {@link #setChapters(java.util.List)}.
     * 
     * @param chapter
     * @param metadata Source metadata for chapter data. In order to retain rest
     * of the chapters, the metadata for the item are necessary.
     */
    public void removeChapter(Chapter chapter, Metadata metadata) {
        List<Chapter> cs = metadata.getChapters();
        if(cs.remove(chapter)) setChapters(cs);
    }
    
    /** @param val the year to set  */
    public void setYear(String val) {
        setField(FieldKey.YEAR, val);
    }
    
    /** 
     * Do not use. Used as color field.
     * @param val custom1 field value to set  */
    public void setCustom1(String val) {
        setField(FieldKey.CUSTOM1, val);
    }
    
    /** 
     * Do not use. Used for chapters.
     * @param val custom1 field value to set  */
    public void setCustom2(String val) {
        setField(FieldKey.CUSTOM2, val);
    }
    
    /** @param val custom3 field value to set  */
    public void setCustom3(String val) {
        setField(FieldKey.CUSTOM3, val);
    }
    
    /** @param val custom4 field value to set  */
    public void setCustom4(String val) {
        setField(FieldKey.CUSTOM4, val);
    }
    
    /** @param val custom5 field value to set  */
    public void setCustom5(String val) {
        setField(FieldKey.CUSTOM5, val);
    }
    
    /** sets field */
    private void setField(FieldKey field, String val) {
        try {
            boolean e = val == null || val.isEmpty();
            if (e) tag.deleteField(field);
            else tag.setField(field, val);
            fields_changed++;
        } catch (KeyNotFoundException ex) {
            Log.info(field.toString() + " field not found.");
        } catch (FieldDataInvalidException ex) {
            Log.info("Invalid " + field.toString() + " field data.");
        } catch (UnsupportedOperationException ex) {
            Log.info("Unsupported operation.");
        }
    }
    
/*******************************************************************************/
    
    /** 
     * Writes all changes to tag.
     * @return true if data were written to tag or false if tag didnt change,
     * either because there was nothing to change or writing failed.
     */
    private boolean write() {
        // write changes to tag
        String f = (fields_changed == 1) ? "field" : "fields";
        Log.deb("Writing " + fields_changed + f + " to tag for: " + getURI() + ".");
        
        // do nothing if nothing to write
        if (!hasFields()) return false;  
        
        try {      
            // save tag
            try {
                audioFile.commit();
            } catch (Throwable ex) {
                if (PlaylistManager.isSameItemPlaying(this)) {
                    PLAYBACK.suspend();
                    audioFile.commit();
                    PLAYBACK.activate();
                } else {
                    throw ex;
                }
            }            
            
            Log.deb("Saving tag for " + getURI() + " finished successfuly.");
            return true;
            
        // } catch (CannotWriteException | UnableToRenameFileException e) {
        } catch (Throwable e) {
            Log.err("Can not write to tag for file: " + audioFile.getFile().getPath() + e);
            return false;
        }
    }
    
    /**
     * Finds out how many fields this writer needs to commit.
     * @return number of fields that will be written to 
     */
    public int fields() {
        return fields_changed;
    }
    
    /**
     * Returns true if nonempty - if there are fields that need to be commited.
     * More formally returns fields() != 0.
     */
    public boolean hasFields() {
        return fields() != 0;
    }
    
    /**
     * Resets all fields to allow reuse of this object. Committing immediately
     * after invoking this method will have no effect. Committing also calls this
     * method automatically.
     */
    public void reset() {
        file = null;
        audioFile = null;
        tag = null;
        fields_changed = 0;
        isWriting.set(false);
    }
    
    public void reset(Item i) {
        file = i.getFile();
        audioFile = readAudioFile(file);
        tag = audioFile.getTagOrCreateAndSetDefault();
        fields_changed = 0;
        isWriting.set(false);
    }
    
/******************************************************************************/
    
    public static <I extends Item> void use(I item, Consumer<MetadataWriter> setter) {
        use(singletonList(item), setter);
    }
    
    public static <I extends Item> void use(List<I> items, Consumer<MetadataWriter> setter) {
        use(items, setter, null);
    }
    
    public static <I extends Item> void use(List<I> items, Consumer<MetadataWriter> setter, Consumer<List<Metadata>> action) {
        runNew(()-> {
            MetadataWriter w = new MetadataWriter();
            for(I i : items) {
                w.reset(i);
                setter.accept(w);
                w.write();
            }

            MetadataReader.readMetadata(items, (ok,metas) -> {
                if (ok) {
                    if(action!=null) action.accept(metas);
                    runNew(() -> Player.refreshItemsWithUpdatedBgr(metas));
                }
            });
        });
    }
    
    public static <I extends Item> void use(I item, Consumer<MetadataWriter> setter, Consumer<Boolean> action) {
        runNew(()-> {
            MetadataWriter w = new MetadataWriter();
            w.reset(item);
            setter.accept(w);
            boolean b = w.write();
            runLater(() -> action.accept(b));
            
            MetadataReader.readMetadata(singletonList(item), (ok,metas) -> {
                if (ok) runNew(() -> Player.refreshItemsWithUpdatedBgr(metas));
            });
        });
    }
    
    /**
     * Rates item.
     * @param item to useToRate.
     * @param rating <0-1> representing percentage of the rating, 0 being minimum
     * and 1 maximum possible rating for current item. Value outside range will
     * be ignored.
     */
    public static void useToRate(Metadata item, double rating) {
        use(item, w->w.setRatingPercent(rating));
        App.use(Notifier.class, n -> n.showTextNotification("Song rating changed to: " + rating, "Update"));
    }
    
}
