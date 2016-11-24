package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by L on 07/11/2016.
 */

/**
 * MediaAdapter provides an adapter backed by a MediaStore content provider.
 * MediaAdapter cung cấp adapter được hỗ trợ bỡi mediaStore content provider
 * Nó tạo ra một hay 2 dòng text view đơn giản để hiển thị trên mỗi media element.
 *
 *  Filtering(lọc) được hỗ trơ.
 *  Cụ thể hơn thì ta có thêm limiting.Limiting thì tách biệt hoàn toàn với filterring
 *  Một filer mới thì sẽ không xóa active filter
 *  Nhưng limitting thì được thiết kế chỉ để hiển thị duy nhất các media thuộc về
 *      một group cụ thểm,ví dụ như chỉ duy nhất những bài hát(songs) từ một nghệ sĩ(artist)
 *      sẽ được hiển thị
 *  Xem thêm getLimiter và setLimiter để biết thêm chi tiết

 */
public class MediaAdapter extends BaseAdapter
    implements LibraryAdapter
        , View.OnClickListener
        , SectionIndexer
{

    private static final Pattern SPACE_SPLIT = Pattern.compile("\\s+");

    private static final String SORT_MAGIC_PLAYCOUNT = "__PLAYCOUNT_SORT";
    /**
     * The string to use for length==0 db fields
     */
    private static final String DB_NULLSTRING_FALLBACK = "???";
    /**
     * Context to use
     */
    private final Context mContext;
    /**
     * The library activity to use
     */
    private final LibraryActivity mActivity;
    /**
     * The Inflater to use
     */
    private final LayoutInflater mInflater;
    /**
     * The current data
     */
    private Cursor mCursor;
    /**
     * The type of media represented by this adapter. Must be one of the
     * MediaUtils.FIELD_* constants. Determines which content provider to query for
     * media and what fields to display.
     */
    private final int mType;
    /**
     * The Uri of the content provider backing this adapter
     */
    private Uri mStore;

    /**
     * The fields to use from the content provider. The last field will be
     * displayed in the MediaView, as will the first field if there are
     * multiple fields. Other fields will be used for searching.
     */
    private String[] mFields;
    /**
     * The collation keys corresponding to each field. If provided, these are
     * used to speed up sorting and filtering.
     */
    private String[] mFieldKeys;
    /**
     * The columns to query from the content provider
     */
    private String[] mProjection;
    /**
     * A limiter is used for filtering. The intention is to restrict items
     * displayed in the list to only those of a specific artist or album, as
     * selected through an expander arrow in a broader MediaAdapter list.
     */
    private Limiter mLimiter;
    /**
     * The constraint used for filtering,set by the searchBox
     */
    private String mConstraint;
    /**
     * The sort order for the use with buildSongQuery().
     */
    private String mSongSort;
    /**
     * The human-readable descriptions for each sort mode.
     */
    private int[] mSortEntries;
    /**
     * An array ORDER BY expressions for each sort mode. %1$s is replaced by
     * ASC or DESC as appropriate before being passed to the query.
     */
    private String[] mSortValues;
    /**
     * The index of the current of the current sort mode in mSortValues, or
     * the inverse of the index (in which case sort should be descending
     * instead of ascending).
     */
    private int mSortMode;
    /**
     * If true, show the expander button on each row.
     */
    private boolean mExpandable;
    /**
     * Defines the media type to use for this entry
     * Setting this to MediaUtils.TYPE_INVALID disables cover artwork
     */
    private int mCoverCacheType;
//    /**
//     * Alphabet to be used for {@link SectionIndexer}. Populated in {@link #buildAlphabet()}.
//     */
//    private List<SectionIndex> mAlphabet = new ArrayList<>(512);

    /**
     * Construct a MediaApdater representing the given <code>type</code>
     * of media.
     *
     * @param context
     * @param type
     * @param limiter
     * @param activity
     */
    public MediaAdapter(Context context, int type, Limiter limiter, LibraryActivity activity){
        this.mContext = context;
        this.mType = type;
        this.mLimiter = limiter;
        this.mActivity = activity;

        if(mActivity != null)
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        else
            mInflater = null;

        // Use media type + base id as cache key combination
        mCoverCacheType = mType;
        String coverCacheKey = BaseColumns._ID;

        switch (type){
            case MediaUtils.TYPE_ARTIST:
                mStore = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Artists.ARTIST};
                mFieldKeys = new String[] {MediaStore.Audio.Artists.ARTIST_KEY};
                mSongSort = MediaUtils.DEFAULT_SORT;
                mSortEntries = new int[] { R.string.name, R.string.number_of_tracks };
                mSortValues = new String[] { "artist_key %1$s", "number_of_tracks %1$s,artist_key %1$s" };
                break;
            case MediaUtils.TYPE_ALBUM:
                mStore = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Albums.ALBUM};
                mFieldKeys = new String[] {MediaStore.Audio.Albums.ALBUM_KEY,MediaStore.Audio.Albums.ARTIST};
                mSongSort = MediaUtils.ALBUM_SORT;
                mSortEntries = new int[] { R.string.name, R.string.artist_album, R.string.year
                        , R.string.number_of_tracks, R.string.date_added };
                mSortValues = new String[] { "album_key %1$s", "artist_key %1$s,album_key %1$s"
                        , "minyear %1$s,album_key %1$s", "numsongs %1$s,album_key %1$s", "_id %1$s" };
                break;
            case MediaUtils.TYPE_SONG:
                mStore = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                mFields = new String[] { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST };
                mFieldKeys = new String[] { MediaStore.Audio.Media.TITLE_KEY, MediaStore.Audio.Media.ALBUM_KEY
                        , MediaStore.Audio.Media.ARTIST_KEY };
                mSortEntries = new int[] { R.string.name, R.string.artist_album_track, R.string.artist_album_title,
                        R.string.artist_year, R.string.album_track,
                        R.string.year, R.string.date_added, R.string.song_playcount };
                mSortValues = new String[] { "title_key %1$s", "artist_key %1$s,album_key %1$s,track"
                        , "artist_key %1$s,album_key %1$s,title_key %1$s",
                        "artist_key %1$s,year %1$s,album_key %1$s, track", "album_key %1$s,track",
                        "year %1$s,title_key %1$s","_id %1$s", SORT_MAGIC_PLAYCOUNT };
                // Songs covers are cached per-album
                mCoverCacheType = MediaUtils.TYPE_ALBUM;
                coverCacheKey = MediaStore.Audio.Albums.ALBUM_ID;
                break;
            case MediaUtils.TYPE_PLAYLIST:
                mStore = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                mFields = new String[] { MediaStore.Audio.Playlists.NAME };
                mFieldKeys = null;
                mSortEntries = new int[] { R.string.name, R.string.date_added };
                mSortValues = new String[] { "name %1$s", "date_added %1$s" };
                mExpandable = true;
                break;
            case MediaUtils.TYPE_GENRE:
                mStore = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
                mFields = new String[] { MediaStore.Audio.Genres.NAME };
                mFieldKeys = null;
                mSortEntries = new int[] { R.string.name };
                mSortValues = new String[] { "name %1$s" };
                break;
            default:
                throw new IllegalArgumentException("Invalid value for type: " + type);
        }


        //Create projection members match with fields members above respectively
        //But with _ID at first and coverCacheKey second
        mProjection = new String[mFields.length + 2];
        mProjection[0] = BaseColumns._ID;
        mProjection[1] = coverCacheKey;
        for(int i = 0;i < mFields.length;i++){
            mProjection[i + 2] = mFields[i];
        }
    }

    /**
     * Returns first sort column for this adapter. Ensure {@link #mSortMode} is correctly set
     * prior to calling this.
     *
     * @return string representing sort column to be used in projection.
     * 		   If the column is binary, returns its human-readable counterpart instead.
     */
    private String getFirstSortColumn(){
        int mode = mSortMode < 0 ? ~mSortMode : mSortMode;//get current sort mode
        String column = SPACE_SPLIT.split(mSortValues[mode])[0];
        if(column.endsWith("_key")){
            column = column.substring(0,column.length() - 4);
        }

        return column;
    }

    public void setExpandable(boolean expandable){
        if(expandable != mExpandable){
            mExpandable = expandable;
            notifyDataSetChanged();
        }
    }

    /**
     *  Build the query to run with runQuery().
     * @param projection The columns to be queried.
     * @param returnSongs
     * @return
     */
    private QueryTask buildQuery(String[] projection, boolean returnSongs) {
        String constraint = mConstraint;
        Limiter limiter = mLimiter;

        StringBuilder selection = new StringBuilder();
        String selectionArgs[] = null;

        int mode = mSortMode;
        String sortDir;
        if(mode < 0){
            mode = ~mode;
            sortDir = "DESC";
        }
        else sortDir = "ASC";

        String sortStringRow = mSortValues[mode];
        String[] enrichedProjection = null;
        //Magic sort mode:sort by playcount
        if(sortStringRow == SORT_MAGIC_PLAYCOUNT) {
            //implement for sorting form database with song counthelper
        }
        else{
            //enrich projection with sort column to build alphabet later
            enrichedProjection = Arrays.copyOf(projection,projection.length + 1);
            enrichedProjection[projection.length] = getFirstSortColumn();

            if(returnSongs && mType != MediaUtils.TYPE_SONG){
                //We are in a non-song adapter but requested to return song-sorting
                //can only be done by using the adapters default sort mode
                sortStringRow = mSongSort;
            }
        }


        //Sort value sẽ được add vào khi build query
        //Thay cái %1$s bằng DESC or ASC
        String sort = String.format(sortStringRow,sortDir);

        if(returnSongs || mType == MediaUtils.TYPE_SONG)
            selection.append(MediaStore.Audio.Media.IS_MUSIC + "AND length(_data)");


        //Câu truy vấn selection
        //Ví dụ ta đang ở tab album mà serch từ khóa sontung m-tp thì
        //  WHERE album_key || artist_key LIKE  sontung AND album_key || artist_key LIKE  m-tp
        if (constraint != null && constraint.length() != 0) {
            String[] needles;
            String[] keySource;

            //  Nếu ta đang sử dụng sorting keys,thì cần phải thay constraint
            //  thành danh sách các collation keys,neeys ko,thì chỉ cần cắt
            //  constraint mà ko cần thay đổi gì
            //  Sử dụng callation keys để thực hiện các thao tác trên chuỗi
            //  được nhanh hơn
            if (mFieldKeys != null) {
                String colKey = MediaStore.Audio.keyFor(constraint);
                String spaceColKey = DatabaseUtils.getCollationKey(" ");
                //neddles là list các từ ta cần tìm liên quan
                //được cắt ra bởi khoảng trắng
                needles = colKey.split(spaceColKey);
                keySource = mFieldKeys;
            } else {
                needles = SPACE_SPLIT.split(constraint);
                keySource = mFields;
            }

            //  Tạo ra mảng selectionArgs dựa trên
            //  số từ khóa được cắt ra từ constraint
            int size = needles.length;
            selectionArgs = new String[size];

            //  Tạo khóa để query
            StringBuilder keys = new StringBuilder(20);
            //  Gắn _ID vào
            keys.append(keySource[0]);
            //  Gắn tất cả các trường có trong fileds để so sánh
            for (int j = 1; j != keySource.length; ++j) {
                keys.append("||");
                keys.append(keySource[j]);
            }


            //Câu truy vấn selection sẽ được append
            //với mỗi needdles add vào selectionArgs tương ứng
            for (int j = 0; j != needles.length; ++j) {
                selectionArgs[j] = '%' + needles[j] + '%';

                // If we have something in the selection args (i.e. j > 0), we
                // must have something in the selection, so we can skip the more
                // costly direct check of the selection length.
                if (j != 0 || selection.length() != 0)
                    selection.append(" AND ");
                selection.append(keys);
                selection.append(" LIKE ?");
            }
        }
        /**
         *
         */
        QueryTask query;
        if(mType == MediaUtils.TYPE_GENRE && !returnSongs) {
            query = MediaUtils.buildGenreExcludeEmptyQuery(enrichedProjection, selection.toString(),
                    selectionArgs, sort);
        } else if (limiter != null && limiter.type == MediaUtils.TYPE_GENRE) {
            // Genre is not standard metadata for MediaStore.Audio.Media.
            // We have to query it through a separate provider. : /
            query = MediaUtils.buildGenreQuery((Long)limiter.data, enrichedProjection,  selection.toString(), selectionArgs, sort, mType, returnSongs);
        } else {
            if (limiter != null) {
                if (selection.length() != 0)
                    selection.append(" AND ");
                //Gán phần câu truy vấn data vào cho selection
                selection.append(limiter.data);
            }
            query = new QueryTask(mStore, enrichedProjection, selection.toString(), selectionArgs, sort);
            if (returnSongs) // force query on song provider as we are requested to return songs
                query.uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return query;
    }

    @Override
    public Cursor query() {
        return buildQuery(mProjection, false).runQuery(mContext.getContentResolver());
    }

    @Override
    public void commitQuery(Object data) {
        changeCursor((Cursor)data);
    }

    /**
     * Build the query for all the songs represented by this adapter,for adding
     * to the timeline
     * @param projection
     * @return
     */
    public QueryTask buildSongQuery(String[] projection){
        QueryTask query = buildQuery(projection, true);
        query.type = mType;
        return query;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public int getMediaTypes() {
        return mType;
    }

    @Override
    public void setLimiter(Limiter limiter) {
        mLimiter = limiter;
    }

    @Override
    public Limiter getLimiter() {
        return mLimiter;
    }

    @Override
    public Limiter buildLimiter(long id) {
        String[] fields;
        Object data;

        Cursor cursor = mCursor;
        if(cursor == null)
            return null;
        for(int i = 0,count = cursor.getCount();i != count;i++){
            cursor.moveToPosition(i);
            if(cursor.getLong(0) == id)
                break;
        }

        switch (mType){
            case MediaUtils.TYPE_ARTIST:
                fields = new String[] {cursor.getString(2)};
                data = String.format("%s=%d",MediaStore.Audio.Media.ARTIST_ID,id);
                break;
            case MediaUtils.TYPE_ALBUM:
                fields = new String[]{cursor.getString(3),cursor.getString(2)};
                data = String.format("%s=%d",MediaStore.Audio.Media.ALBUM_ID,id);
                break;
            case MediaUtils.TYPE_GENRE:
                fields = new String[] {cursor.getString(2)};
                data = id;
                break;
            default:
                throw new IllegalStateException("getLimiter() is not supported for media type: " + mType);
        }

        return new Limiter(mType, fields, data);
    }

    @Override
    public void setFilters(String filters) {
        mConstraint = filters;
    }


    @Override
    public void clear() {
        changeCursor(null);
    }

    @Override
    public Intent createData(View row) {
        return null;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public Object[] getSections() {
        return new Object[0];
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    /**
     * Set new cursor for this adapter
     * The older will be closed
     * @param cursor
     */
    public void changeCursor(Cursor cursor){
        Cursor old = mCursor;
        mCursor = cursor;
        if (cursor == null) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }
        if (old != null) {
            old.close();
        }
    }
}
