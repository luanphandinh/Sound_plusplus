package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    /**
     * Builds a query that will return all the songs in the genre with the
     * given id.
     *
     * @param id The id of the genre in MediaStore.Audio.Genres.
     * @param projection The columns to query.
     * @param selection The selection to pass to the query, or null.
     * @param selectionArgs The arguments to substitute into the selection.
     * @param sort The sort order.
     * @param type The media type to query and return
     * @param returnSongs returns matching songs instead of `type' if true
     */
    public static QueryTask buildGenreQuery(long id, String[] projection, String selection, String[] selectionArgs, String sort, int type, boolean returnSongs)
    {
        // Note: This function works on a raw sql query with way too much internal
        // knowledge about the mediaProvider SQL table layout. Yes: it's ugly.
        // The reason for this mess is that android has a very crippled genre implementation
        // and does, for example, not allow us to query the albumbs beloging to a genre.

        Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        String[] clonedProjection = projection.clone(); // we modify the projection, but this should not be visible to the caller
        String sql = "";
        String authority = "audio";

        if (type == TYPE_ARTIST)
            authority = "artist_info";
        if (type == TYPE_ALBUM)
            authority = "album_info";

        // Our raw SQL query includes the album_info table (well: it's actually a view)
        // which shares some columns with audio.
        // This regexp should matche duplicate column names and forces them to use
        // the audio table as a source
        final String _FORCE_AUDIO_SRC = "(^|[ |,\\(])(_id|album(_\\w+)?|artist(_\\w+)?)";

        // Prefix the SELECTed rows with the current table authority name
        for (int i=0 ;i<clonedProjection.length; i++) {
            if (clonedProjection[i].equals("0") == false) // do not prefix fake rows
                clonedProjection[i] = (returnSongs ? "audio" : authority)+"."+clonedProjection[i];
        }

        sql += TextUtils.join(", ", clonedProjection);
        sql += " FROM audio_genres_map_noid, audio" + (authority.equals("audio") ? "" : ", "+authority);
        sql += " WHERE(audio._id = audio_id AND genre_id=?)";

        if (selection != null && selection.length() > 0)
            sql += " AND("+selection.replaceAll(_FORCE_AUDIO_SRC, "$1audio.$2")+")";

        if (type == TYPE_ARTIST)
            sql += " AND(artist_info._id = audio.artist_id)" + (returnSongs ? "" : " GROUP BY artist_info._id");

        if (type == TYPE_ALBUM)
            sql += " AND(album_info._id = audio.album_id)" + (returnSongs ? "" : " GROUP BY album_info._id");

        if (sort != null && sort.length() > 0)
            sql += " ORDER BY "+sort.replaceAll(_FORCE_AUDIO_SRC, "$1audio.$2");

        // We are now turning this into an sql injection. Fun times.
        clonedProjection[0] = sql +" --";

        QueryTask result = new QueryTask(uri, clonedProjection, selection, selectionArgs, sort);
        result.type = TYPE_GENRE;
        return result;
    }

    /**
     * Creates a {@link QueryTask} for genres. The query will select only genres that have at least
     * one song associated with them.
     *
     * @param projection The fields of the genre table that should be returned.
     * @param selection Additional constraints for the query (added to the WHERE section). '?'s
     * will be replaced by values in {@code selectionArgs}. Can be null.
     * @param selectionArgs Arguments for {@code selection}. Can be null. See
     * {@link android.content.ContentProvider#query(Uri, String[], String, String[], String)}
     * @param sort How the returned genres should be sorted (added to the ORDER BY section)
     * @return The QueryTask for the genres
     */
    public static QueryTask buildGenreExcludeEmptyQuery(String[] projection, String selection, String[] selectionArgs, String sort) {
		/*
		 * An example SQLite query that we're building in this function
			SELECT DISTINCT _id, name
			FROM audio_genres
			WHERE
				EXISTS(
					SELECT audio_id, genre_id, audio._id
					FROM audio_genres_map, audio
					WHERE (genre_id == audio_genres._id)
						AND (audio_id == audio._id))
			ORDER BY name DESC
		 */
        Uri uri = MediaStore.Audio.Genres.getContentUri("external");
        StringBuilder sql = new StringBuilder();
        // Don't want multiple identical genres
        sql.append("DISTINCT ");

        // Add the projection fields to the query
        sql.append(TextUtils.join(", ", projection)).append(' ');

        sql.append("FROM audio_genres ");
        // Limit to genres that contain at least one valid song
        sql.append("WHERE EXISTS( ")
                .append("SELECT audio_id, genre_id, audio._id ")
                .append("FROM audio_genres_map, audio ")
                .append("WHERE (genre_id == audio_genres._id) AND (audio_id == audio._id) ")
                .append(") ");

        if (!TextUtils.isEmpty(selection))
            sql.append(" AND(" + selection + ") ");

        if(!TextUtils.isEmpty(sort))
            sql.append(" ORDER BY ").append(sort);

        // Ignore the framework generated query
        sql.append(" -- ");
        String[] injectedProjection = new String[1];
        injectedProjection[0] = sql.toString();

        // Don't pass the selection/sort as we've already added it to the query
        return new QueryTask(uri, injectedProjection, null, selectionArgs, null);
    }
    /**
     * return the collation key
     * @param name
     * @return the collation key
     */
    public static String getCollationKey(String name) {
        byte [] arr = getCollationKeyInBytes(name);
        try {
            return new String(arr, 0, getKeyLen(arr), "ISO8859_1");
        } catch (Exception ex) {
            return "";
        }
    }

    //Không biết khu này để làm gì
    private static byte[] getCollationKeyInBytes(String name) {
        if (mColl == null) {
            mColl = Collator.getInstance();
            mColl.setStrength(Collator.PRIMARY);
        }
        return mColl.getCollationKey(name).toByteArray();
    }
    private static Collator mColl = null;

    private static int getKeyLen(byte[] arr) {
        if (arr[arr.length - 1] != 0) {
            return arr.length;
        } else {
            // remove zero "termination"
            return arr.length-1;
        }
    }

    public static QueryTask buildQuery(int type,long id,String[] projection,String selection){
        switch (type) {
            case TYPE_ARTIST:
            case TYPE_ALBUM:
            case TYPE_SONG:
                Log.d("TestPlay","BuildSongQuery");
                return buildMediaQuery(type, id, projection, selection);
            default:
                throw new IllegalArgumentException("Specified type not valid: " + type);
        }
    }

    /**
     * Build cái query để return tất cả bài hát thông qua các tham số được truyền vào
     * @param type
     * @param id
     * @param projection
     * @param select
     * @return
     */
    private static QueryTask buildMediaQuery(int type,long id,String[] projection,String select){
        Uri media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        StringBuilder selection = new StringBuilder();

        switch (type){
            case TYPE_SONG:
                Log.d("TestPlay","Append id" + id);
                selection.append(MediaStore.Audio.Media._ID);
                break;
            case TYPE_ARTIST:
                selection.append(MediaStore.Audio.Media.ARTIST_ID);
                break;
            case TYPE_ALBUM:
                selection.append(MediaStore.Audio.Media.ALBUM_ID);
                break;
            default:
                throw new IllegalArgumentException("Invalid type specified: " + type);
        }

        selection.append("=");
        selection.append(id);
        selection.append(" AND length(_data) AND "+MediaStore.Audio.Media.IS_MUSIC);

        if (select != null) {
            selection.append(" AND ");
            selection.append(select);
        }

        QueryTask result = new QueryTask(media, projection, selection.toString(), null, null);
        result.type = type;
        return result;
    }

    /**
     * Shuffle danh sách các bài hát sử dụng Collections.shuffle().
     * @param list
     * @param albumShuffle
     */
    public static void shuffle(List<Song> list,boolean albumShuffle){
        int size = list.size();
        if (size < 2)
            return;

        Random random = getRandom();

        if (albumShuffle) {
            //Sort danh sách các bài hát dược truyền vào theo
            //thứ tự từ album sau đó là bài hát
            List<Song> tempList = new ArrayList<Song>(list);
            Collections.sort(tempList);

            //Build một cái map dựa trên albumId
            //Để có thông tin các album là duy nhất và map với index của bài hát đầu tiên trong album
            Map<Long, Integer> albumStartIndices = new HashMap<Long, Integer>();
            int index = 0;
            //Chạy hết tempList,với mỗi albumId thì sẽ có
            //index bắt đầu của bài hát đàu tiên trong album đó trong templist
            for (Song song : tempList) {
                if (!albumStartIndices.containsKey(song.albumId)) {
                    albumStartIndices.put(song.albumId, index);
                }
                index++;
            }

            //Lấy albumId từ map ra,sau đó shuffle
            List<Long> shuffledAlbums = new ArrayList<Long>(albumStartIndices.keySet());
            Collections.shuffle(shuffledAlbums, random);

            //Build danh sách các bài hát từ danh sách album
            list.clear();
            for (Long albumId : shuffledAlbums) {
                int songIndex = albumStartIndices.get(albumId);
                Song song = tempList.get(songIndex);
                do {
                    list.add(song);
                    songIndex++;
                    if (songIndex < size) {
                        song = tempList.get(songIndex);
                    } else {
                        break;
                    }
                } while (albumId == song.albumId);
            }
        }else{
            Collections.shuffle(list,random);
        }
    }

    /**
     * Cached random instance.
     */
    private static Random sRandom;

    /**
     * Trả về một cái ranodm instance
     */
    public static Random getRandom()
    {
        if (sRandom == null)
            sRandom = new Random();
        return sRandom;
    }

}
