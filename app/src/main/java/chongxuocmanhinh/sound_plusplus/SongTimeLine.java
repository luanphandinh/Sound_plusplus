package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;

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

    private final Context mContext;
    /**
     * Tất cả các bài hát hiện đang được chứa trong songTimeLine.Mỗi đối tượng
     * Song là duy nhất,dù cho có cùng tham chiếu đến 1 media
     */
    private ArrayList<Song> mSongs = new ArrayList<Song>(12);

    private Song mSavedPrevious;
    private Song mSavedCurrent;
    private Song mSavedNext;
    private int mCurrentPos;
    private int mSavedPos;
    private int mSavedSize;

    /**
     * Interface dùng để phản ứng với các thay đổi của songTimeLine
     */
    public interface Callback {
        /**
         * Called when an active song in the timeline is replaced by a method
         * other than shiftCurrentSong()
         *
         * @param delta The distance from the current song. Will always be -1,
         * 0, or 1.
         * @param song The new song at the position
         */
        void activeSongReplaced(int delta, Song song);

        /**
         * Called when the timeline state has changed and should be saved to
         * storage.
         */
        void timelineChanged();

        /**
         * Called when the length of the timeline has changed.
         */
        void positionInfoChanged();
    }

    /**
     *Callback hieenjt ại nếu có
     */
    private Callback mCallback;

    public SongTimeLine(Context context)
    {
        mContext = context;
    }

    /**
     * Chạy cái queyr được truyền vào,sau đó add các kết quả trả về vào song timeline
     * Tùy trường hợp mà add,nếu nghệ sĩ và album thì ta sẽ add tất cả bài hát liên quan vào trong
     * danh sách hiện đang được giữ
     *
     * @param context Context mà mình dùng
     * @param queryTask queryTask dùng để chạy.biến mode được khởi tạo
     *                  với một trong các giá trị của SongTimeLine.MODE_*
     *                  biến type và data cũng cần phải được khởi tạo đựa trên mode được đưa vào
     * @return
     */
    public int addSongs(Context context,QueryTask queryTask){
        Log.d("Testtt","songtimeline run query");
        Cursor cursor = queryTask.runQuery(context.getContentResolver());
        if(cursor == null)
            return 0;

        int mode = queryTask.mode;
        int type = queryTask.type;
        long data = queryTask.data;

        int count = cursor.getCount();

        /**
         * Nếu ko query được gì thì thoats
         */
        if(count == 0) {
            Log.d("TestShowQueue","count = 0");
            cursor.close();
            return 0;
        }

        ArrayList<Song> timeline = mSongs;

        synchronized (this) {
            switch (mode){
                case MODE_ENQUEUE:

                case MODE_PLAY:
                    timeline.clear();
                    mCurrentPos = 0;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid mode: " + mode);
            }

            int start = timeline.size();

            /**
             * jumpSong dùng để set bài hát được play đầu tiên tùy vào các trường hợp
             * sau khi được query
             */
            Song jumpSong = null;
            int addAtPos = mCurrentPos + 1;

            /**
             * Kiếm tra xem addAtPos có lớn hơn size của timeline hay ko
             */
            if(addAtPos > start){
                addAtPos = start;
            }

            /**
             * Vòng lặp dùng để đưa tất cả bài hát được query vào danh sách để play
             */
            for(int index = 0 ;index != count;index++){
                cursor.moveToPosition(index);

                Song song = new Song(-1);
                song.populate(cursor);
                if(song.isFilled() == false) {
                    continue;
                }

                timeline.add(addAtPos++,song);
                Log.d("Testtt","song : " + mSongs.get(index).path);

                if(jumpSong == null){

                }
            }
            cursor.close();
            broadcastChangedSongs();
//            cursor.moveToPosition(0);
//            Song song = new Song(-1);
//            song.populate(cursor);
//            timeline.add(0,song);
//            cursor.close();
        }

        changed();
        return 1;
    }
    /**
     * Broadcasts that the timeline state has changed.
     */
    private void changed()
    {
        if (mCallback != null)
            mCallback.timelineChanged();
    }

    /**
     * Trả về bài hát ở vị trí delta được đặt so với bài hát hiện tại
     * ví dụ trả về bài trước bài hiện tại thì delta = -1 bài đằng sau thì delta = 1
     * @param delta
     * @return
     */
    public Song getSong(int delta){
        Log.d("TestShowQueue","songtimeline getSong");
        //Kiểm tra xem delta có hợp lệ hay không
        Assert.assertTrue(delta >= -1 && delta <= 1);

        ArrayList<Song> timeline = mSongs;
        Song song;

        synchronized (this) {
            int pos = mCurrentPos + delta;
            int size = timeline.size();
            if (pos < 0) {
                if (size == 0)
                    return null;
                song = timeline.get(Math.max(0, size - 1));
            }
            else if (pos > size) {
                return null;
            }
            else if (pos == size){
                if(size == 0)
                    return null;
                else song = timeline.get(0);
            }
            else
                song = timeline.get(pos);
        }
        if (song == null)
            // we have no songs in the library
            return null;
        return song;
    }

    /**
     * Broadcast the active songs that have changed since the last call to
     * saveActiveSongs()
     *
     */
    private void broadcastChangedSongs()
    {
        Log.d("TestShowQueue","broadcast change song");
        if (mCallback == null) return;

        Song current = getSong(0);

        mCallback.activeSongReplaced(0, current);
    }

    public void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    /**
     * Trả về bài hát tại vị trí trong queue.
     * @param id
     * @return
     */
    public Song getSongByQueuePosition(int id){
        Song song = null;
        synchronized (this) {
            if (mSongs.size() > id)
                song = mSongs.get(id);
        }
        return song;
    }
    /**
     * Trả về vị trí của bài hát hiện tại trong timeline
     * @return
     */
    public int getPosition(){
        return mCurrentPos;
    }

    public int getLength(){
        return mSongs.size();
    }
}
