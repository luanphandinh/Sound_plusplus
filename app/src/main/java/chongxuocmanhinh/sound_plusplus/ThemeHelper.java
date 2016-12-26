package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;

/**
 * Created by L on 24/12/2016.
 */
public class ThemeHelper {

    /**
     * Gọi Context.setTheme() với tên theme được truyền vào
     * Tự động chuyển theme sang theme được yêu cầu
     */
    final public static int setTheme(Context context, int theme)
    {
        if(usesHoloTheme() == false) {
            TypedArray ar = context.getResources().obtainTypedArray(R.array.theme_styles);
            int themeBase = ar.getResourceId(getSelectedTheme(context), R.style.SoundPlusPlusBase);
            ar.recycle();

            switch (theme) {
                case R.style.Playback:
                    theme = themeBase + (R.style.Playback - R.style.SoundPlusPlusBase);
                    break;
                case R.style.Library:
                    theme = themeBase + (R.style.Library - R.style.SoundPlusPlusBase);
                    break;
                case R.style.BackActionBar:
                    theme = themeBase + (R.style.BackActionBar - R.style.SoundPlusPlusBase);
                    break;
                case R.style.PopupDialog:
                    theme = themeBase + (R.style.PopupDialog - R.style.SoundPlusPlusBase);
                    break;
                default:
                    throw new IllegalArgumentException("setTheme() called with unknown theme!");
            }
        }
        context.setTheme(theme);
        return theme;
    }

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

    /**
     * Trả về  TRUE if thiết bị sử dụng  HOLO (android 4) theme
     */
    final public static boolean usesHoloTheme() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
    }

    /**
     * Returns the user-selected theme id from the shared peferences provider
     *
     * @param context the context to use
     * @return integer of the selected theme
     */
    final private static int getSelectedTheme(Context context) {
        SharedPreferences settings = PlaybackService.getSettings(context);
        return Integer.parseInt(settings.getString(PrefKeys.SELECTED_THEME, PrefDefaults.SELECTED_THEME));
    }

    /**
     * Hacky function to get the colors needed to draw the default cover
     * These colors should actually be attributes, but getting them programatically
     * is a big mess
     */
    final public static int[] getDefaultCoverColors(Context context) {
        int[] colors_holo_yolo         = { 0xff000000, 0xff606060, 0xff404040, 0x88000000 };
        int[] colors_material_light    = { 0xffeeeeee, 0xffd6d7d7, 0xffd6d7d7, 0x55ffffff };
        int[] colors_material_dark     = { 0xff303030, 0xff606060, 0xff404040, 0x33ffffff };
        int[] colors_marshmallow_light = { 0xfffafafa, 0xffd6d7d7, 0xffd6d7d7, 0x55ffffff };
        int[] colors_marshmallow_dark  = colors_material_dark;
        if (usesHoloTheme()) // pre material device
            return colors_holo_yolo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return usesDarkTheme(context) ? colors_marshmallow_dark : colors_marshmallow_light;
        // else
        return usesDarkTheme(context) ? colors_material_dark : colors_material_light;
    }

    /**
     * Returns TRUE if we should use the dark material theme,
     * Returns FALSE otherwise - always returns FALSE on pre-5.x devices
     */
    final private static boolean usesDarkTheme(Context context)
    {
        boolean useDark = false;
        if(usesHoloTheme() == false) {
            useDark = (getSelectedTheme(context) % 2 != 0); // odd values are always dark
        }
        return useDark;
    }


}
