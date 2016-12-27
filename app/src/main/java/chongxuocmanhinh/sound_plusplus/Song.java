package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 07/11/2016.
 */

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

/**
 * Class thể hiên cho bài hát của MediaStore.bao gồm các dữ liệu cơ bản để nhận các bài hát từ
 * MediaStore

 */
public class Song implements Comparable<Song>{
    /**
     * Bài hát được chọn ngẫu nhiên từ nhóm các bài hát nào đó
     */
    public static final int FLAG_RANDOM = 0x1;
    /**
     * Ko có cờ này thì bài hát ko có hình ảnh cover,
     * nếu có cờ này thì bài hát có thể có hoặc có thể không
     */
    public static final int FLAG_NO_COVER = 0x2;
    /**
     * Số lượng của cờ
     */
    public static final int FLAG_COUNT  = 2;
    /**
     * The cache instance.
     */
    private static CoverCache sCoverCache = null;

    public static final String[] EMPTY_PROJECTION = {
            MediaStore.Audio.Media._ID,
    };

    public static final String[] FILLED_PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
    };

    public static final String[] EMPTY_PLAYLIST_PROJECTION = {
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
    };


    public static final String[] FILLED_PLAYLIST_PROJECTION = {
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members.DATA,
            MediaStore.Audio.Playlists.Members.TITLE,
            MediaStore.Audio.Playlists.Members.ALBUM,
            MediaStore.Audio.Playlists.Members.ARTIST,
            MediaStore.Audio.Playlists.Members.ALBUM_ID,
            MediaStore.Audio.Playlists.Members.ARTIST_ID,
            MediaStore.Audio.Playlists.Members.DURATION,
            MediaStore.Audio.Playlists.Members.TRACK,
    };


    /**
     * The cache instance..
     *
     *
     */

    /**
     * Id of this song in the MediaStore
     */
    public long id;

    /**
     * Id of this song's album in the MediaStore
     */
    public long albumId;

    /**
     * Id of this artist's song in the MediaStore
     */
    public long artistId;

    /**
     * Path to the data for this song
     */
    public String path;

    /**
     * Song title
     */
    public String title;

    /**
     *Album name
     */
    public String album;

    /**
     * Artist name
     */
    public String artist;

    /**
     * Length of the song in milisecond
     */
    public long duration;

    /**
     * The position of the song in its album
     */
    public int trackNumber;

    /**
     * Song flags Currently {@link #FLAG_RANDOM} or {@link #FLAG_NO_COVER}.
     */
    public int flags;

    /**
     * Khởi tạo giá trị ban đầu của bài hát với id.
     * Gọi hàm populate để gán thông tin vào bài hát
     */
    public Song(long id){this.id = id;}

    /**
     * Khởi tạo giá trị ban đầu của bài hát với id và flags.
     * Gọi hàm populate để gán thông tin vào bài hát
     */
    public Song(long id,int flags){this.id = id;this.flags = flags;}

    /**
     * return true if this song was retrieved from randomSong().
     */
    public boolean isRandom(){return (this.flags & FLAG_RANDOM) != 0;}

    /**
     * Return true if the song is filled
     */
    public  boolean isFilled(){return (id != -1 && path != null);}

    /**
     * Populate dữ liệu từ cusor được cung cấp
     * @param cursor Cursor queried with FILLED_PROJECTION projection.
     */
    public void populate(Cursor cursor) {
        id = cursor.getLong(0);
        path = cursor.getString(1);
        title = cursor.getString(2);
        album = cursor.getString(3);
        artist = cursor.getString(4);
        albumId = cursor.getLong(5);
        artistId = cursor.getLong(6);
        duration = cursor.getLong(7);
        trackNumber = cursor.getInt(8);
    }

    /**
     * Get the id of the given song.
     * @param song The song to get id from.
     * @return The id,0 if the given song is null.
     */
    public static long getId(Song song) {
        if(song == null)
            return 0;
        return song.id;
    }

    /**
     * So sánh 2 bài hát với nhau
     * @param otherSong
     * @return
     */
    @Override
    public int compareTo(Song otherSong) {
        if (albumId == otherSong.albumId)
            return trackNumber - otherSong.trackNumber;
        if (albumId > otherSong.albumId)
            return 1;
        return -1;
    }

    /**
     * Query the large album art for this song
     * @param context A context to use.
     * @return The album art or null if no album art could be found
     */
    public Bitmap getSmallCover(Context context) {
        return getCoverInternal(context, CoverCache.SIZE_SMALL);
    }

    /**
     * Internal implementation of getCover
     *
     * @param context A context to use.
     * @param size The desired cover size
     * @return The album art or null if no album art could be found
     */
    private Bitmap getCoverInternal(Context context, int size) {
        if (CoverCache.mCoverLoadMode == 0 || id == -1 || (flags & FLAG_NO_COVER) != 0)
            return null;

        if (sCoverCache == null)
            sCoverCache = new CoverCache(context.getApplicationContext());

        Bitmap cover = sCoverCache.getCoverFromSong(this, size);

        if (cover == null)
            flags |= FLAG_NO_COVER;
        return cover;
    }
}
