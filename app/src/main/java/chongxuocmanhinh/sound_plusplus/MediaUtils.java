package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 07/11/2016.
 */
public class MediaUtils {
    /**
     * A special invalid media type
     */
    public static final int TYPE_INVALID = -1;

    /**
     * Type indicating an id represents an artist
     */
    public static final int TYPE_ARTIST = 0;

    /**
     * Type indicating an id represents an album.
     */
    public static final int TYPE_ALBUM = 1;

    /**
     * Type indicating an id represents a song.
     */
    public static final int TYPE_SONG = 2;

    /**
     * Type indicating an id represents a playlist.
     */
    public static final int TYPE_PLAYLIST = 3;

    /**
     * Type indicating ids represent genres.
     */
    public static final int TYPE_GENRE = 4;

    /**
     * Special type for files and folders. Most methods do not accept this type
     * since files have no MediaStore id and require special handling.
     */
    public static final int TYPE_FILE = 5;

    /**
     * The number of different valid media types.
     */
    public static final int TYPE_COUNT = 6;

}
