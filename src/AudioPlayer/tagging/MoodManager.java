
package AudioPlayer.tagging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import main.App;
import util.File.FileUtil;
import util.Util;

/**
 * 
 * Maintains list of moods for tagging.
 * 
 * @author Plutonium_
 */
public class MoodManager {
    
    private static final File MOODS_FILE = new File(App.DATA_FOLDER(), "MoodList.cfg");
    private static final File MOODS_DEF_FILE = new File(App.DATA_FOLDER(), "MoodList_DEFAULT.cfg");
    
    /** List of moods for tagging. Editable. Unsorted.*/
    public static final ObservableSet<String> moods = FXCollections.observableSet();
    
    /** 
     * Set content of the mood list to default. This will remove all custom
     * moods!
     */
    public static void toDefault() {
        loadMoods(MOODS_DEF_FILE);
        save();
    }
    
/******************************************************************************/
    
    public static void initialize() {
        loadMoods(MOODS_FILE);
        moods.addListener((SetChangeListener.Change<? extends String> c) -> {
            moods.forEach(Util::capitalize);
            save();
        });
    }
    
    private static void loadMoods(File f) {
        try {
            moods.clear();
            moods.addAll(Files.readAllLines(f.toPath()));
            moods.forEach(Util::capitalize);
        } catch (IOException ex) {
            Logger.getLogger(MoodManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void save() {
        String content = moods.stream().collect(Collectors.joining("\n"));
        File f = new File(App.DATA_FOLDER(), "MoodList.cfg");
        FileUtil.writeFile(f.getPath(), content);
    }
}
