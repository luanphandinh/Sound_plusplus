package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

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
    /**
     * The default sort order for media queries. First artist, then album, then
     * track number.
     */
    public static final String DEFAULT_SORT = "artist_key,album_key,track";
    /**
     * The default sort order for albums. First the album, then tracknumber
     */
    public static final String ALBUM_SORT = "album_key,track";

    /**
     * Runs a query on the passed content resolver.
     * Catches(and return null on) SecurityException (= user revoked read permission)
     * @param resolver The content resolver to use
     * @param uri the uri to query
     * @param projection the projection to use
     * @param selection the selecttion to use
     * @param selectionArgs the slectionArgs to use
     * @param sortOrder sort order to use
     * @return
     */
    public static Cursor queryResolver(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch(java.lang.SecurityException e) {
            // we do not have read permission - just return a null cursor
        }
        return cursor;
    }
}
