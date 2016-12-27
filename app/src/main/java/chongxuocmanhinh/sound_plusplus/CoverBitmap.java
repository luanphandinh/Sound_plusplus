package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by L on 27/12/2016.
 *
 * Lớp bao gồm các tính năng dùng để tạo ra BitMap hiển thị thoongtin bài hát
 * và album.
 */
public class CoverBitmap {

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


    public static Bitmap generatePlaceholderCover(Context context, int width, int height, String title)
    {
        if (title == null || width < 1 || height < 1)
            return null;

        final float textSize = width * 0.4f;

        title = title.replaceFirst("(?i)^The ", ""); // 'The\s' shall not be a part of the string we are drawing.
        title = title.replaceAll("[ <>_-]", ""); // Remove clutter, so eg. J-Rock becomes JR
        String subText = (title+"  ").substring(0,2);

        // Use only the first char if it is 'wide'
        if(Character.UnicodeBlock.of(subText.charAt(0)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
            subText = subText.substring(0,1);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // Picks a semi-random color from tiles_colors.xml
        TypedArray colors = context.getResources().obtainTypedArray(R.array.letter_tile_colors);
        int color = colors.getColor(Math.abs(title.hashCode()) % colors.length(), 0);
        colors.recycle();
        paint.setColor(color);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        paint.setARGB(255, 255, 255, 255);
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(subText, 0, subText.length(), bounds);

        canvas.drawText(subText, (width/2f)-bounds.exactCenterX(), (height/2f)-bounds.exactCenterY(), paint);
        return bitmap;
    }
}
