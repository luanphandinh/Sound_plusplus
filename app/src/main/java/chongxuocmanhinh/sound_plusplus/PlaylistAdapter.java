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

public class PlaylistAdapter extends CursorAdapter implements Handler.Callback{
    private static final String[] PROJECTION = new String[] {
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
     * @param worker Looper điều khiên worker thread (để query)
     * */
    public PlaylistAdapter(Context context, Looper worker){
        super(context,null,false);

        mContext=context;
        mWorkerHandler=new Handler(worker,this);
        mUiHandler=new Handler(this);
        mInflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    /**
     * Set the id of the backing playlist.
     *
     * @param id The MediaStore id of a playlist.
     */
    public void setPlaylistId(long id)
    {
        mPlaylistId = id;
        mWorkerHandler.sendEmptyMessage(MSG_RUN_QUERY);
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
                changeCursor((Cursor)message.obj);
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.draggable_row,parent,false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        DraggableRow dview = (DraggableRow)view;
        dview.setupLayout(DraggableRow.LAYOUT_DRAGGABLE);
        dview.showDragger(mEditable);

        TextView textView = dview.getTextView();
        textView.setText(cursor.getString(1));
        textView.setTag(cursor.getLong(3));

    }

    /**
     * Query các bài hát trong playlist
     *
     * @param resolver A ContentResolver to query with.
     * @return The resulting cursor.
     */
    private Cursor runQuery(ContentResolver resolver)
    {
        QueryTask query = MediaUtils.buildPlaylistQuery(mPlaylistId, PROJECTION, null);
        return query.runQuery(resolver);
    }


}
