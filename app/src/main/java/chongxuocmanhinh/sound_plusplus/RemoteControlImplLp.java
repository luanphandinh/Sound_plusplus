package chongxuocmanhinh.sound_plusplus;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.session.MediaSession;

/**
 * Created by L on 24/12/2016.
 */
@TargetApi(21)
public class RemoteControlImplLp implements RemoteControl.Client{
    /**
     * Context of this instance
     */
    private Context mContext;
    /**
     * Objects MediaSession handle
     */
    private MediaSession mMediaSession;
    /**
     * Whether the cover should be shown. 1 for yes, 0 for no, -1 for
     * uninitialized.
     */
    private int mShowCover = -1;

    /**
     * Creates a new instance
     *
     * @param context The context to use
     */
    public RemoteControlImplLp(Context context) {
        mContext = context;
    }

    /**
     * Thực hiện việc khởi tạo cho  RemoteControlClient.
     *
     */
    @Override
    public void initializeRemote() {
        //bỏ hết remote
        unregisterRemote();

    }

    @Override
    public void unregisterRemote() {

    }

    @Override
    public void reloadPreference() {

    }

    @Override
    public void updateRemote(Song song, int state, boolean keepPaused) {

    }
}
