package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Created by lordhung on 26/12/2016.
 * Cursor adapter dựa trên Mediastore playlists
 */

public class PlaylistAdapter extends CursorAdapter implements Handler.Callback {
    private static final String[] PROJECTION = new String[]{
            MediaStore.Audio.Playlists.Members._ID,
            MediaStore.Audio.Playlists.Members.TITLE,
            MediaStore.Audio.Playlists.Members.ARTIST,
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members.ALBUM_ID,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER,
    };

    private final Context mContext;
    private final Handler mWorkerHandler;
    private final Handler mUiHandler;
    private final LayoutInflater mInflater;
    private long mPlaylistId;
    private boolean mEditable;

    /**
     * Re-run the query. Should be run on worker thread.
     */
    public static final int MSG_RUN_QUERY = 1;
    /**
     * Update the cursor. Must be run on UI thread.
     */
    public static final int MSG_UPDATE_CURSOR = 2;

    /**
     * Tạo 1 playlist adapter
     *
     * @param context
     * @param worker  Looper điều khiên worker thread (để query)
     */
    public PlaylistAdapter(Context context, Looper worker) {
        super(context, null, false);

        mContext = context;
        mWorkerHandler = new Handler(worker, this);
        mUiHandler = new Handler(this);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Set the id of the backing playlist.
     *
     * @param id mediastore id của playlist .
     */
    public void setPlaylistId(long id) {
        mPlaylistId = id;
        mWorkerHandler.sendEmptyMessage(MSG_RUN_QUERY);
    }

    /**
     * Enable hoặc disable edit mode. Edit mde thêm một drag grabber
     * vào fía bên trái view và 1 nút delete bên fải view
     *
     * @param editable True để enable edit mode
     */
    public void setEditable(boolean editable) {
        mEditable = editable;
        notifyDataSetInvalidated();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_RUN_QUERY: {
                Cursor cursor = runQuery(mContext.getContentResolver());
                mUiHandler.sendMessage(mUiHandler.obtainMessage(MSG_UPDATE_CURSOR, cursor));
                break;
            }
            case MSG_UPDATE_CURSOR:
                changeCursor((Cursor) message.obj);
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.draggable_row, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        DraggableRow dview = (DraggableRow) view;
        dview.setupLayout(DraggableRow.LAYOUT_DRAGGABLE);
        dview.showDragger(mEditable);

        TextView textView = dview.getTextView();
        textView.setText(cursor.getString(1));
        textView.setTag(cursor.getLong(3));

        // thêm coverview cho playlist
        LazyCoverView cover = dview.getCoverView();
        cover.setCover(MediaUtils.TYPE_ALBUM, cursor.getLong(4), null);
    }

    /**
     * Query các bài hát trong playlist
     *
     * @param resolver A ContentResolver to query with.
     * @return The resulting cursor.
     */
    private Cursor runQuery(ContentResolver resolver) {
        QueryTask query = MediaUtils.buildPlaylistQuery(mPlaylistId, PROJECTION, null);
        return query.runQuery(resolver);
    }

    /**
     * Dịch chuyển(lên,xuống) một bài hát trong playlist
     *
     * @param from vị trí ban đầu của bài hát
     * @param to   vị trí đích của bài hát
     */
    public void moveSong(int from, int to) {
        if (from == to)
            return;

        int count = getCount();

        if (to >= count || from >= count)
            // có thể xảy ra khi adapter thay đổi trong khi drag
            return;

        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId);
        Cursor cursor = getCursor();

        int start = Math.min(from, to);
        int end = Math.max(from, to);

        long order;
        if (start == 0)
            order = 0;
        else {
            cursor.moveToPosition(start - 1);
            order = cursor.getLong(5) + 1;
        }

        cursor.moveToPosition(end);
        long endOrder = cursor.getLong(5);

        // xóa những dòng định thay thế
        String[] args = new String[]{
                Long.toString(order), Long.toString(endOrder)
        };
        resolver.delete(uri, "play_order >= ? AND play_order <= ?", args);

        // tạo những dòng mới
        ContentValues[] values = new ContentValues[end - start + 1];
        for (int i = start, j = 0; i <= end; ++i, ++j, ++order) {
            cursor.moveToPosition(i == to ? from : i > to ? i - 1 : i + 1);
            ContentValues value=new ContentValues(2);
            value.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,Long.valueOf(order));
            value.put(MediaStore.Audio.Playlists.Members.AUDIO_ID,cursor.getLong(3));
            values[j]=value;
        }

        // chèn nhũng dòng mới
        resolver.bulkInsert(uri,values);
        changeCursor(runQuery(resolver));
    }

    /**
     * Khi user fling bài hát về phía trái -> xóa bài đó, update lại view
     * */
    public void removeSong(int position){
        ContentResolver resolver=mContext.getContentResolver();
        Uri uri=MediaStore.Audio.Playlists.Members.getContentUri("external",mPlaylistId);
        // xóa song
        resolver.delete(ContentUris.withAppendedId(uri,getItemId(position)),null,null);
        // query lại
        mUiHandler.sendEmptyMessage(MSG_RUN_QUERY);
    }


}
