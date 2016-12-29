package chongxuocmanhinh.sound_plusplus;

import android.content.SharedPreferences;

/**
 * Created by L on 29/12/2016.
 */
enum Action {
    /**
     * Dummy action: do nothing.
     */
    Nothing,
    /**
     * Open the library activity.
     */
    Library,
    /**
     * If playing music, pause. Otherwise, start playing.
     */
    PlayPause,
    /**
     * Skip to the next song.
     */
    NextSong,
    /**
     * Go back to the previous song.
     */
    PreviousSong,
    /**
     * Skip to the first song from the next album.
     */
    NextAlbum,
    /**
     * Skip to the last song from the previous album.
     */
    PreviousAlbum,
    /**
     * Cycle the repeat mode.
     */
    Repeat,
    /**
     * Cycle the shuffle mode.
     */
    Shuffle,
    /**
     * Enqueue the rest of the current album.
     */
    EnqueueAlbum,
    /**
     * Enqueue the rest of the songs by the current artist.
     */
    EnqueueArtist,
    /**
     * Enqueue the rest of the songs in the current genre.
     */
    EnqueueGenre,
    /**
     * Clear the queue of all remaining songs.
     */
    ClearQueue,
    /**
     * Displays the queue
     */
    ShowQueue,
    /**
     * Toggle the controls in the playback activity.
     */
    ToggleControls,
    /**
     * Seek 10 seconds forward
     */
    SeekForward,
    /**
     * Seek 10 seconds back
     */
    SeekBackward;

    /**
     * Retrieve an action from the given SharedPreferences.

     * @param prefs The SharedPreferences instance to load from.
     * @param key The preference key to load.
     * @param def The value to return if the key is not found or cannot be loaded.
     * @return The loaded action or def if no action could be loaded.
     */
    public static Action getAction(SharedPreferences prefs, String key, Action def)
    {
        try {
            String pref = prefs.getString(key, null);
            if (pref == null)
                return def;
            return Enum.valueOf(Action.class, pref);
        } catch (Exception e) {
            return def;
        }
    }
}
