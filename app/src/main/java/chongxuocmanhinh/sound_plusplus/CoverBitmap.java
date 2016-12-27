package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by L on 27/12/2016.
 *
 * Lớp bao gồm các tính năng dùng để tạo ra BitMap hiển thị thoongtin bài hát
 * và album.
 */
public class CoverBitmap {
    /**
     * Vẽ cover ở dưới và thông tin của bài hát ở trên.
     */
    public static final int STYLE_OVERLAPPING_BOX = 0;
    /**
     * Vẽ cover ở trên hoặc bên trái với song info  ở dưới hoặc bên phải (tùy vào
     * orientation của thiết bị).
     */
    public static final int STYLE_INFO_BELOW = 1;
    /**
     * Không có song info,chỉ có cover.
     */
    public static final int STYLE_NO_INFO = 2;

    private static int TEXT_SIZE = -1;
    private static int TEXT_SIZE_BIG;
    private static int PADDING;
    private static int TEXT_SPACE;
    private static Bitmap SONG_ICON;
    private static Bitmap ALBUM_ICON;
    private static Bitmap ARTIST_ICON;

    /**
     * Khởi tạo các text size thông thường.
     *
     * @param context A context to use.
     */
    private static void loadTextSizes(Context context)
    {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        TEXT_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, metrics);
        TEXT_SIZE_BIG = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, metrics);
        PADDING = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, metrics);
        TEXT_SPACE = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, metrics);
    }

    /**
     *Khởi tạo icon bitmaps
     *
     * @param context A context to use.
     */
    private static void loadIcons(Context context)
    {
        Resources res = context.getResources();
        SONG_ICON = BitmapFactory.decodeResource(res, R.drawable.ic_musicnote);
        ALBUM_ICON = BitmapFactory.decodeResource(res, R.drawable.ic_disk);
        ARTIST_ICON = BitmapFactory.decodeResource(res, R.drawable.ic_microphone);
    }

}
