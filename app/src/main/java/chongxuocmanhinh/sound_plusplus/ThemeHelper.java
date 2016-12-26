package chongxuocmanhinh.sound_plusplus;

import android.os.Build;

/**
 * Created by L on 24/12/2016.
 */
public class ThemeHelper {

    /**
     * Hàm hỗ trợ cho việc lấy icon nút play cho notification
     * Notification ko phụ thuộc vào theme,nhưng phụ thuộc vào API level
     */
    final public static int getPlayButtonResource(boolean playing)
    {
        int playButton = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android >= 5.0 sử dụng bản màu đen
            playButton = playing ? R.drawable.widget_pause : R.drawable.widget_play;
        } else {
            playButton = playing ? R.drawable.pause : R.drawable.play;
        }
        return playButton;
    }


}
