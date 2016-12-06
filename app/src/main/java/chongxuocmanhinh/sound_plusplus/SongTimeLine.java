package chongxuocmanhinh.sound_plusplus;

import android.content.Context;

/**
 * Created by L on 06/12/2016.
 */
/*
    Class này dùng để chứa danh sách các bài hát đang được play
    Hỗ trợ việc repeat và shuffle cho trình nghe nhạc
    Có các hàm dùng để fetch nhiều bài hát hơn từ MediaStore.
 */
public class SongTimeLine {
    /**
     * Stop playback.
     *
     */
    public static final int FINISH_STOP = 0;
    /**
     * Chơi nhạc lại từ bài hát đầu tiên.
     *
     */
    public static final int FINISH_REPEAT = 1;
    /**
     * Lặp lại bài hát hiện tại
     * Nhấn nút next hay previous sẽ chuyển bài hát như bình thường
     * chỉ trường hợp bài hát nào được play tới cuối thì lặp lại bài hát đó
     */
    public static final int FINISH_REPEAT_CURRENT = 2;
    /**
     * Dừng lại tại bài hát hiện tại
     * Nhấn nút next hay previous sẽ chuyển bài hát như bình thường
     * Chỉ cho phép bài hát được play tới cuối thì dừng.
     */
    public static final int FINISH_STOP_CURRENT = 3;
    /**
     * Thêm bài hát ngẫu nhiên vào danh sách đang phát.
     */
    public static final int FINISH_RANDOM = 4;
    /**
     * Danh sách các icon tương ứng với các actions.
     */
    public static final int[] FINISH_ICONS =
            { R.drawable.repeat_inactive, R.drawable.repeat_active,
                    R.drawable.repeat_current_active, R.drawable.stop_current_active, R.drawable.random_active };

    /**
     * Clear timeline và sử dụng duy nhất bài hát được cung cấp.
     *
     */
    public static final int MODE_PLAY = 0;

    /**
     * Clear hàn đợi(queue) và add các bài hát vào sau bài hát hiện tại.
     *
     */
    public static final int MODE_FLUSH_AND_PLAY_NEXT = 1;
    /**
     * Thêm các bài hát vào cuối timline.clearing random songs.
     *
     */
    public static final int MODE_ENQUEUE = 2;
}
