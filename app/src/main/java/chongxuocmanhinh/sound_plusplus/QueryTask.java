package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by L on 08/11/2016.
 */
public class QueryTask {
    public Uri uri;
    public final String[] projection;
    public final String selection;
    public final String[] selectionArgs;
    public String sortOrder;
    /**
     * One of SongTimeline.MODE_*.
     */
    public int mode;
    /**
     * Type of the group being query
     */
    public int type;
    /**
     * Data. Cần dữ liệu phụ thuộc vào giá trị của mode
     */
    public long data;

    public QueryTask(Uri uri,String[] projection, String selection, String[] selectionArgs,String sortOrder) {
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    /**
     * Run the query.Should be called on the background thread.
     *
     * @param resolver
     * @return
     */
    public Cursor runQuery(ContentResolver resolver){
        return MediaUtils.queryResolver(resolver,uri, projection, selection, selectionArgs, sortOrder);
    }
}
