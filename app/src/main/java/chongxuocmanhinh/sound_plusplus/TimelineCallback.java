package chongxuocmanhinh.sound_plusplus;

/**
 * Created by L on 14/12/2016.
 * interface này được implement bởi hầu hết các activity trong apps.
 * Để thực hiện các công việc liên quan đến thay đổi bài hát,random sort....
 * Service sẽ dựa vào interface này để xử lý tương ứng với các listview.
 *
 *
 */
public interface TimelineCallback {
    /**
     * Sets bài hát đang được bật này
     * Hàm setSong được sử dụng khá nhiều trên app.mỗi lần người dùng click vào một bài hát
     * hay play hết tất cả bài hát,hàm setSong sẽ được service sử dụng để set bài hát đang được active
     * lên hết tất cả các playback có implement interface này
     */
    void setSong(long uptime, Song song);
    /**
     * Nếu nhưu timeline(danh sách các bài hát hay liên quan đến viêc chơi nhạc) bị thay đổi
     * thì service sẽ gọi tới hàm này trên tất cả các activity có implement interface này
     */
    void onTimelineChanged();

}
