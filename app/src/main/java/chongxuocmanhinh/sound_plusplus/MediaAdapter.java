package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by L on 07/11/2016.
 */

/**
 * MediaAdapter provides an adapter backed by a MediaStore content provider.
 * It generates simple one- or two-line text views to display each media
 * element.
 *
 * Filtering is supported, as is a more specific type of filtering referred to
 * as limiting. Limiting is separate from filtering; a new filter will not
 * erase an active filter. Limiting is intended to allow only media belonging
 * to a specific group to be displayed, e.g. only songs from a certain artist.
 * See getLimiter and setLimiter for details.
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
                mFieldKeys = new String[] {MediaStore.Audio.Albums.ALBUM_KEY};
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
     *
     * @param projection
     * @param returnSongs
     * @return
     */
    private QueryTask buildQuery(String[] projection, boolean returnSongs) {

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
        return 0;
    }

    @Override
    public void setLimiter(Limiter limiter) {

    }

    @Override
    public Limiter getLimiter() {
        return null;
    }

    @Override
    public Limiter buildLimiter(long id) {
        return null;
    }

    @Override
    public void setFilters(String filters) {
        mConstraint = filters;
    }

    @Override
    public Object query() {
        return null;
    }

    @Override
    public void commitQuery(Object data) {

    }

    @Override
    public void clear() {

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
}
