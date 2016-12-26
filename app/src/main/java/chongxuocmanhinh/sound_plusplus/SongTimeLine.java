package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import junit.framework.Assert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Created by L on 06/12/2016.
 */
/*
    Class này dùng để chứa danh sách các bài hát đang được play
    Hỗ trợ việc repeat và shuffle cho trình nghe nhạc
    Có các hàm dùng để fetch nhiều bài hát hơn từ MediaStore.
    PlaybackService là một interface của lớp này
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

    /**
     *  Giống với playmode nhưng ta sẽ cho bài hát có id được play đầu tiên
     *  bằng cách bỏ các bài hát trước đó từ queary ra ngoài sau đó sẽ gắn
     *  lại các bài hát đó vào sau hàng đợi.Nếu có nhiều bài hát có cùng id
     *  thì lấy bài hát đầu tiên
     *
     *  Truyền id qua QueryTask.data.
     *
     * @see SongTimeLine#addSongs(Context, QueryTask)
     */
    public static final int MODE_PLAY_ID_FIRST = 4;
    /**
     *  Giống với enqueue mode nhưng ta sẽ cho bài hát có id được play đầu tiên
     *  bằng cách bỏ các bài hát trước đó từ queary ra ngoài sau đó sẽ gắn
     *  lại các bài hát đó vào sau hàng đợi.Nếu có nhiều bài hát có cùng id
     *  thì lấy bài hát đầu tiên
     *
     *  Truyền id qua QueryTask.data.
     *
     * @see SongTimeLine#addSongs(Context, QueryTask)
     */
    public static final int MODE_ENQUEUE_ID_FIRST = 5;

    /**
     * Enqueues the result as next item(s)
     *
     * Pass the position in QueryTask.data.
     *
     * @see SongTimeLine#addSongs(Context, QueryTask)
     */
    public static final int MODE_ENQUEUE_AS_NEXT = 7;

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
     * Tắt shuffle .
     *
     * @see SongTimeLine#setShuffleMode(int)
     */
    public static final int SHUFFLE_NONE = 0;
    /**
     * Random thứ tự của các bài hát.
     *
     * @see SongTimeLine#setShuffleMode(int)
     */
    public static final int SHUFFLE_SONGS = 1;
    /**
     * Random thứ tự các album,giữ lại thứ tự của các bài hát trong album
     *
     * @see SongTimeLine#setShuffleMode(int)
     */
    public static final int SHUFFLE_ALBUMS = 2;

    /**
     * Icons tương tứng với từng SHUFFLE_MODE.
     */
    public static final int[] SHUFFLE_ICONS =
            { R.drawable.shuffle_inactive, R.drawable.shuffle_active, R.drawable.shuffle_album_active };


    /**
     * Di chuyển vị trí hiện tại về album trước đó.
     *
     * @see SongTimeLine#shiftCurrentSong(int)
     */
    public static final int SHIFT_PREVIOUS_ALBUM = -2;
    /**
     * Di chuyển vị trí hiện tại tới bài hát trước đó.
     *
     * @see SongTimeLine#shiftCurrentSong(int)
     */
    public static final int SHIFT_PREVIOUS_SONG = -1;
    /**
     * Noop
     * @see SongTimeLine#shiftCurrentSong(int)
     */
    public static final int SHIFT_KEEP_SONG = 0;
    /**
     * Di chuyển vị trí hiện tại tới bài hát tiếp theo.
     *
     * @see SongTimeLine#shiftCurrentSong(int)
     */
    public static final int SHIFT_NEXT_SONG = 1;
    /**
     * Di chuyển vị trí hiện tại về album sau đó.
     *
     * @see SongTimeLine#shiftCurrentSong(int)
     */
    public static final int SHIFT_NEXT_ALBUM = 2;

    /**
     * Hành động xảy ra khi đi đến bài nhạc cuối cùng trong danh sách
     * hoặc kết thúc 1 bài nhạc
     */
    private int mFinishAction;
    /**
     * Được sử dụng để xáo thứ tự các bài hát hoặc album
     */
    private int mShuffleMode;
    /**
     * Danh sách được chuẩn bị(shuffled) thể thay thế cho playlist.
     */
    private ArrayList<Song> mShuffleCache;
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
            saveActiveSongs();
            switch (mode){
                case MODE_ENQUEUE:
                case MODE_ENQUEUE_AS_NEXT:
                    break;
                case MODE_FLUSH_AND_PLAY_NEXT:
                    timeline.subList(mCurrentPos + 1,timeline.size()).clear();
                    break;
                case MODE_PLAY:
                case MODE_PLAY_ID_FIRST:
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
            if(addAtPos > start || mode != MODE_ENQUEUE_AS_NEXT){
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
     * Di chuyển tới bài háy hay album tiếp theo,trước đó
     * @param delta
     * @return
     */
    public Song shiftCurrentSong(int delta)
    {
        synchronized (this) {
            if (delta == SHIFT_KEEP_SONG) {
                // ko làm gì hết,xuống duwois sẽ lấy bài hát hiện tại ra
            }
            else if (delta == SHIFT_PREVIOUS_SONG || delta == SHIFT_NEXT_SONG) {
                shiftCurrentSongInternal(delta);
            }
        }

        if (delta != SHIFT_KEEP_SONG)
            changed();
        return getSong(0);
    }

    /**
     * implement cho hàm shiftCurrentSong. Làm hết tất cả trừ việc broadcast timlineChanged()
     *
     * @param delta
     */
    private void shiftCurrentSongInternal(int delta){
        int pos = mCurrentPos + delta;
        if(mFinishAction != FINISH_RANDOM && pos == mSongs.size()){
            pos = 0;
        }else if (pos < 0){
            pos = Math.max(0,mSongs.size() - 1);
        }

        mCurrentPos = pos;
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

        Song previous = getSong(-1);
        Song current = getSong(0);
        Song next = getSong(+1);

        if (Song.getId(mSavedPrevious) != Song.getId(previous))
            mCallback.activeSongReplaced(-1, previous);
        if (Song.getId(mSavedNext) != Song.getId(next))
            mCallback.activeSongReplaced(1, next);
        if (Song.getId(mSavedCurrent) != Song.getId(current))
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

    public Song setCurrentQueuePosition(int pos){
        synchronized (this){
            saveActiveSongs();
            mCurrentPos = pos;
            broadcastChangedSongs();
        }
        changed();
        return getSong(0);
    }
    /**
     * Gán giá trị khi kết thúc danh sách nhạc hay bài hát.Phải là một trong
     * SongTimeline.FINISH_* (stop, repeat, or add random song).
     */
    public void setFinishAction(int action){
        saveActiveSongs();
        mFinishAction = action;
        broadcastChangedSongs();
        changed();
    }

    public void setShuffleMode(int mode){
        if(mode == mShuffleMode)
            return;

        synchronized (this){
            saveActiveSongs();
            mShuffleMode = mode;
            if(mode != SHUFFLE_NONE && mFinishAction != FINISH_RANDOM && !mSongs.isEmpty()){
                ArrayList<Song> songs = getShuffleTimeLine(false);
                mCurrentPos = songs.indexOf(mSavedCurrent);
                mSongs = songs;
            }
            broadcastChangedSongs();
        }

        changed();
    }

    /**
     * Clear hàng đợi đằng sau nào hát hiện tại
     */
    public void clearQueue(){
        synchronized (this){
            saveActiveSongs();
            if(mCurrentPos + 1 < mSongs.size())
                mSongs.subList(mCurrentPos + 1,mSongs.size()).clear();
            broadcastChangedSongs();
        }
        changed();
    }

    /**
     * Xóa hết danh sách nhạc
     */
    public void emptyQueue(){
        synchronized (this){
            saveActiveSongs();
            mSongs.clear();
            mCurrentPos = 0;
            broadcastChangedSongs();
        }
        changed();
    }

    public void removeSongPosition(int pos){
        synchronized (this){
            ArrayList<Song> songs = mSongs;

            if(songs.size() < pos)
                return;

            saveActiveSongs();

            songs.remove(pos);
            if(pos < mCurrentPos)
                mCurrentPos--;
            if(getSong(1) == null)//quay về đầu danh sách nếu đây là bài hát cuối cùng
                mCurrentPos = 0;
            broadcastChangedSongs();
        }
        changed();
    }

    /**
     * Di chuyển bài hát trong timline tới vị trí mới
     * @param from vị trí bắt đầu move
     * @param to vị trí sau khi move
     */
    public void moveSongPosition(int from,int to){
        synchronized (this){
            ArrayList<Song> songs = mSongs;

            if (songs.size() <= from || songs.size() <= to)
                return;

            saveActiveSongs();

            Song tmp = songs.remove(from);
            songs.add(to,tmp);

            //Nếu bài hát đang được mở mà drag đến chỗ khác
            if (mCurrentPos == from) {
                mCurrentPos = to;
            } else if (from > mCurrentPos && to <= mCurrentPos) {
                mCurrentPos++;
            } else if (from < mCurrentPos && to >= mCurrentPos) {
                mCurrentPos--;
            }

            broadcastChangedSongs();
        }
        changed();

    }
    /**
     * Trả về danh sách đã được xáo(dựa trên shufflemode) của timeline.
     * Giá trị trả về sẽ được cached.
     * @param cached
     * @return
     */
    private ArrayList<Song> getShuffleTimeLine(boolean cached){
        if (cached == false)
            mShuffleCache = null;

        if(mShuffleCache == null){
            ArrayList<Song> songs = new ArrayList<Song>(mSongs);
            MediaUtils.shuffle(songs,mShuffleMode == SHUFFLE_ALBUMS);
            mShuffleCache = songs;
        }

        return new ArrayList<Song>(mShuffleCache);
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

    public boolean isEndQueue(){
        synchronized (this){
            return mFinishAction == FINISH_STOP && mCurrentPos == mSongs.size() - 1;
        }
    }

    public void saveActiveSongs(){
        mSavedPrevious = getSong(-1);
        mSavedCurrent = getSong(0);
        mSavedNext = getSong(+1);
        mSavedPos = mCurrentPos;
        mSavedSize = mSongs.size();
    }

    public int getFinishAction(){
        return mFinishAction;
    }

    /**
     * Lưu danh sách các bài hát được chạy và trạng thái xuống stream được đưa vào
     * @param out
     */
    public void writeState(DataOutputStream out) throws IOException{
        synchronized (this){
            ArrayList<Song> songs = mSongs;

            int size = songs.size();
            //Ghi số lương bài hát xuống
            out.writeInt(size);
            //Ghi mỗi id tương tứng với mỗi bài hát xuống
            for(int i =0;i != size;i++){
                Song song = songs.get(i);
                if(song == null){
                    out.writeLong(-1);
                } else {
                    out.writeLong(song.id);
                    out.writeInt(song.flags);
                }
            }

            //Ghi bài hát đang chạy và finishaction hiện tại xuống
            out.writeInt(mCurrentPos);
            out.writeInt(mFinishAction);
        }
    }

    public void readState(DataInputStream in) throws IOException{
        synchronized (this){
            //Đọc số lượng bài hát
            int n = in.readInt();
            if(n > 0){
                ArrayList<Song> songs = new ArrayList<Song>(n);
                /**
                 * Đưa hết tất cả các ids của các bài hát vào câu lệnh selection
                 //Câu truy vấn có thể xem thêm tại
                 * @see MediaAdapter#buildQuery(String[], boolean)
                 * Chỉ khác ở đây phần selection ta sẽ
                 *  Where ... _ID IN(id0,id1,id2,id3,...,idn-1)
                 */
                StringBuilder selection = new StringBuilder("_ID IN (");
                for (int i = 0; i != n; ++i) {
                    long id = in.readLong();
                    if (id == -1)
                        continue;

                    // Để index vào hàng flags để chừng ta sort lại
                    int flags = in.readInt() & ~(~0 << Song.FLAG_COUNT) | i << Song.FLAG_COUNT;
                    songs.add(new Song(id, flags));

                    if (i != 0)
                        selection.append(',');
                    selection.append(id);
                }
                selection.append(')');

                //Vì khi ta query ra thì sẽ trả về danh sách được xếp theo ids
                //Nên trước đó ta cần xếp lại songs theo id
                Collections.sort(songs, new IdComparator());

                ContentResolver resolver = mContext.getContentResolver();
                Uri media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                Cursor cursor = MediaUtils.queryResolver(resolver, media, Song.FILLED_PROJECTION, selection.toString(), null, "_id");
                if (cursor != null) {
                    if (cursor.getCount() != 0) {
                        cursor.moveToNext();
                        //Đầu tiên ta sẽ chạy từ songs
                        Iterator<Song> it = songs.iterator();
                        while (it.hasNext()) {
                            Song e = it.next();
                            //Vì trong danh sách có thể có các bài hát trùng nhau
                            //và đã được sort lại theo ids tăng dần
                            //nên với các bài hát trùng id trong songs
                            //ta sẽ tiếp tục populate
                            while (cursor.getLong(0) < e.id && !cursor.isLast())
                                cursor.moveToNext();
                            if (cursor.getLong(0) == e.id)
                                e.populate(cursor);
                        }
                    }

                    cursor.close();

                    //Ta có thể query ra sai,hoặc ko thể populate một số
                    //bài hát,bỏ hết nó đi
                    Iterator<Song> it = songs.iterator();
                    while (it.hasNext()) {
                        Song e = it.next();
                        if (e.isFilled() == false) {
                            it.remove();
                        }
                    }

                    // Đào về vị trí được lưu lúc ban đầu bằng cách so sánh flags đã chứa index
                    Collections.sort(songs, new FlagComparator());

                    mSongs = songs;
                }
            }

            mCurrentPos = Math.min(mSongs == null ? 0 : mSongs.size(), Math.abs(in.readInt()));
            mFinishAction = in.readInt();

            // Guard against corruption
            if (mFinishAction < 0 || mFinishAction >= FINISH_ICONS.length)
                mFinishAction = 0;
        }
    }

    public static class IdComparator implements Comparator<Song>{

        @Override
        public int compare(Song song1, Song song2) {
            if(song1.id == song2.id)
                return 0;
            if(song1.id > song2.id)
                return 1;
            return -1;
        }
    }

    public static class FlagComparator implements Comparator<Song>{

        @Override
        public int compare(Song song1, Song song2) {
            return song1.flags - song2.flags;
        }
    }
}
