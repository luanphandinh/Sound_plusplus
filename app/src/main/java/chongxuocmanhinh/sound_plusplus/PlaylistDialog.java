package chongxuocmanhinh.sound_plusplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.app.DialogFragment;

/**
 * Created by lordhung on 27/12/2016.
 * dialog playlist show lên cho phép thêm song vào playlist
 */

public class PlaylistDialog extends DialogFragment implements DialogInterface.OnClickListener{

    /**
     * Data structure
     * */
    public class Data{
        public String name;
        public long id;
        public Intent sourceIntent;
        public MediaAdapter allSource;
    }

    /**
     * callback interface của playlistdialog
     * */
    public interface Callback{
        void updatePlaylistFromPlaylistDialog(PlaylistDialog.Data data);
    }

    /**
     * Một class dùng để implement callback interface
     * */
    private Callback mCallback;

    /**
     * Data để đưa vào callback
     * */
    private PlaylistDialog.Data mData;

    /**
     * Mảng chúa tên tất cả các playlists được tìm thấy
     * */
    private String[] mItemName;
    /**
     * Mảng chúa values tất cả các playlists được tìm thấy
     * */
    private long[] mItemValue;

    /**
     * Index của nút 'create_playlist'
     * */
    private final int BUTTON_CREATE_PLAYLIST=0;

    /**
     * Tạo một playlist dialog mới để tập hợp một playlist sử dụng 1 intent
     * */
    PlaylistDialog(Callback callback, Intent intent, MediaAdapter allSource){
        mCallback=callback;
        mData=new PlaylistDialog.Data();
        mData.sourceIntent=intent;
        mData.allSource=allSource;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstaceState){
        Cursor cursor=Playlist.queryPlaylists(getActivity().getContentResolver());
        if(cursor==null)
            return null;

        int count=cursor.getCount();
        mItemName=new String[1+count];
        mItemValue=new long[1+count];

        // Index 0 luôn luôn là 'New Playlist...'
        mItemName[0]="New playlist...";
        mItemValue[0]=-1;

        for(int i=0;i<count;i++){
            cursor.moveToPosition(i);
            mItemValue[1+i]=cursor.getLong(0);
            mItemName[1+i]=cursor.getString(1);
        }

        // Khi tất cả các name đều có xong: có thể show dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setTitle("Add to playlist...").setItems(mItemName,this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case BUTTON_CREATE_PLAYLIST:
                PlaylistInputDialog newDialog=new PlaylistInputDialog(new PlaylistInputDialog.Callback() {
                    @Override
                    public void onSuccess(String input) {
                        mData.id = -1;
                        mData.name = input;
                        mCallback.updatePlaylistFromPlaylistDialog(mData);
                    }
                },"",R.string.create);
                newDialog.show(getFragmentManager(),"PlaylistInputDialog");
                break;
            default:
                mData.id=mItemValue[which];
                mData.name=mItemName[which];
                mCallback.updatePlaylistFromPlaylistDialog(mData);
        }
        dialog.dismiss();
    }
}
