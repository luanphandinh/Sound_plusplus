package chongxuocmanhinh.sound_plusplus;

import java.util.ArrayList;

/**
 * Created by lordhung on 27/12/2016.
 * Thao tác cấp phát playlist
 */

public class PlaylistTask {
    /**
     * ID của playlist để cấp phát
     */
    public long playlistId;
    /**
     * tên playlist (used for the toast message)
     */
    public String name;
    /**
     * Populate playlist sử dụng QueryTask
     */
    public QueryTask query;
    /**
     * Populate playlist sử dụng audioIds
     */
    public ArrayList<Long> audioIds;


    public PlaylistTask(long playlistId, String name) {
        this.playlistId = playlistId;
        this.name = name;
    }
}
