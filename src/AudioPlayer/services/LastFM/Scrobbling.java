/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services.LastFM;

import AudioPlayer.tagging.Metadata;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.prefs.Preferences;
import utilities.Log;

/**
 *
 * @author Michal
 */
public class Scrobbling {

    static Session session;

    private final String apiKey;
    private final String secret;

    private final Preferences preferences;

    public Scrobbling() {
        apiKey = acquireApiKey();
        secret = acquireSecret();

        preferences = Preferences.userNodeForPackage(LastFMManager.class);
    }

    protected void updateNowPlaying() {
        Metadata currentMetadata = AudioPlayer.Player.getCurrentMetadata();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );

//        System.out.println("ok: " + (result.isSuccessful() && !result.isIgnored()));
    }

    protected void scrobble(Metadata track) {
        Log.mess("Scrobbling: " + track);
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleResult result = Track.scrobble(
                track.getArtist(),
                track.getTitle(),
                now,
                session);

    }

    protected final String acquireApiKey() {
        return "f429ccceafc6b81a6ffad442cec758c3";
    }

    protected final String acquireSecret() {
        return "8097fcb4a54a9805599060e47ab69561";
    }

    Preferences getPreferences() {
        return preferences;
    }

    Session getSession() {
        return session;
    }
}