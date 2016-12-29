package chongxuocmanhinh.sound_plusplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.EditText;

/**
 * Created by lordhung on 27/12/2016.
 */

public class PlaylistInputDialog extends DialogFragment implements DialogInterface.OnClickListener,TextWatcher{

    public interface Callback{
        void onSuccess(String input);
    }

    /**
     * EditText instance
     * */
    private EditText mEditText;

    /**
     * callback implementing PlaylistInputDialog.Callback interface
     * */
    private Callback mCallback;

    /**
     * Label của nút chấp nhận
     * */
    private int mActionRes;

    /**
     * Initial text để hiển thị
     * */
    private String mInitialText;

    /**
     * Instace của  alert dialog
     * */
    private AlertDialog mDialog;

    /**
     * Tạo instance mới
     * @param callback callback để call back
     * @param initText giá trị init của mEditText
     * @param actionRes label của nút chấp nhận
     * */
    PlaylistInputDialog(Callback callback,String initText,int actionRes){
        mCallback=callback;
        mInitialText=initText;
        mActionRes=actionRes;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        mEditText=new EditText(getActivity());
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        mEditText.addTextChangedListener(this);

        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_playlist_name)
                .setView(mEditText)
                .setPositiveButton(mActionRes,this)
                .setNegativeButton(android.R.string.cancel,this);
        mDialog=builder.create();
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return mDialog;
    }

    /**
     * hàm này dc gọi khi view trở nên visible,
     * nên có thể set nút chấp nhận và request focus
     * */
    public void onStart(){
        super.onStart();
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        mEditText.setText(mInitialText);
        mEditText.requestFocus();
    }

    /**
     * hàm này dc gọi trước khi mEditText bị change
     * */
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Donothing

    }
    /**
     * hàm này dc gọi khi mEditText bị change
     * */
    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
        String string=text.toString();
        if(string.equals(mInitialText))
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        else{
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            ContentResolver resolver=getContext().getContentResolver();
            int res=Playlist.getPlaylist(resolver,string)==-1?mActionRes:R.string.overwrite;
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(res);
        }
    }
    /**
     * hàm này dc gọi sau khi mEditText bị change
     * */
    @Override
    public void afterTextChanged(Editable editable) {
        // Donothing

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_NEGATIVE:
                // Donothing
                break;
            case DialogInterface.BUTTON_POSITIVE:
                mCallback.onSuccess(mEditText.getText().toString());
                break;
            default:
                break;
        }
        dialog.dismiss();
    }
}
