package chongxuocmanhinh.sound_plusplus;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by L on 12/12/2016.
 *
 * Tham khảo thêm phần mediaplayer tại
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 */
public class SoundPlusPLusMediaPlayer extends MediaPlayer {
    private Context mContext;
    private String mDataSource;
    private boolean mHasNextMediaPlayer;
    public SoundPlusPLusMediaPlayer(Context context){
        super();
        mContext = context;
    }

    /**
     * Đưa mediaplayer về trạng thái idle
     */
    public void reset() {
        mDataSource = null;
        super.reset();
    }

    /**
     * Đưa mediaplayer tới trạng thái end trong vòng đời
     */
    public void release() {
        mDataSource = null;
        super.release();
    }

    /**
     * CHuyển thằng mediaplayer từ trạng thái idle tới trạng thái Iniialized
     */
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        // The MediaPlayer function expects a file:// like string but also accepts *most* absolute unix paths (= paths with no colon)
        // We could therefore encode the path into a full URI, but a much quicker way is to simply use
        // setDataSource(FileDescriptor) as the framework code would end up calling this function anyways (MediaPlayer.java:1100 (6.0))
        FileInputStream fis = new FileInputStream(path);
        super.setDataSource(fis.getFD());
        fis.close(); // this is OK according to the SDK documentation!
        mDataSource = path;
    }

    /**
     * Returns the configured data source, may be null
     */
    public String getDataSource() {
        return mDataSource;
    }


    /**
     * Sets the next media player data source
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setNextMediaPlayer(SoundPlusPLusMediaPlayer next) {
        super.setNextMediaPlayer(next);
        mHasNextMediaPlayer = (next != null);
    }

    /**
     * Returns true if a 'next' media player has been configured
     * via setNextMediaPlayer(next)
     */
    public boolean hasNextMediaPlayer() {
        return mHasNextMediaPlayer;
    }
}
