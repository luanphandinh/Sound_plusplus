package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 14/12/2016.
 */
public interface TimelineCallback {
    /**
     * Sets the currently active song
     */
    void setSong(long uptime, Song song);
}
