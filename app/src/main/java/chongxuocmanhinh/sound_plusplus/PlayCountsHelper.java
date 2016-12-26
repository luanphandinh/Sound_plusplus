package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by L on 26/12/2016.
 * Tham khảo thêm về cách sử dụng SQLiteOpenHelper tại:
 * https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 */
public class PlayCountsHelper extends SQLiteOpenHelper {

    /**
     * các hằng số và câu lệnh dùng để CREATE TABLE được sử dụng bởi class
     */
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "playcounts.db";
    private static final String TABLE_PLAYCOUNTS = "playcounts";
    private static final String DATABASE_CREATE = "CREATE TABLE "+TABLE_PLAYCOUNTS + " ("
            + "type      INTEGER, "
            + "type_id   BIGINT, "
            + "playcount INTEGER, "
            + "skipcount INTEGER);";
    //tham khảo thêm tại
    //http://www.w3schools.com/sql/sql_create_index.asp
    //cơ bản thì ta tạo 2 cái unique index
    //một cái cho type và 1 cái cho type,type_id
    //việc tạo unique index sẽ khiến cho việc tìm kiếm nhanh và hiệu quả hơn.
    //Việc update sẽ lâu hơn table bình thường vì khi đó index cũng cần update lại,
    //cụ thể là khi add,còn bình thường table này t chỉ dùng cho việc tìm kiếm và udpate playcount nên
    //sẽ không ảnh hưởng nhiều
    private static final String INDEX_UNIQUE_CREATE = "CREATE UNIQUE INDEX idx_uniq ON "+TABLE_PLAYCOUNTS
            + " (type, type_id);";
    private static final String INDEX_TYPE_CREATE = "CREATE INDEX idx_type ON "+TABLE_PLAYCOUNTS
            + " (type);";


    private Context ctx;

    public PlayCountsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        ctx = context;
    }

    //Hàm này sẽ được gọi lần đầu khi database được khởi tạo
    //Đây là nơi mà ta sẽ thực hiện việc khởi tạo database ,table và các khóa liên quan
    //Sau khi được khởi tọa thì database sẽ được cached.
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("TestPlayCounts","onCreate");
        db.execSQL(DATABASE_CREATE);
        db.execSQL(INDEX_UNIQUE_CREATE);
        db.execSQL(INDEX_TYPE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Đếm bài hát được truyền vào đã được nghe hay bị bỏ qua
     * @param song
     * @param played
     */
    public void countSong(Song song,boolean played){
        long id = Song.getId(song);
        final String column = played ? "playcount" : "skipcount";

        //https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html#getWritableDatabase()
        SQLiteDatabase dbh = getWritableDatabase();
        //Nếu bài hát chưa được đưa vào bảng đếm thì insert ko thì ignore nó đi
        dbh.execSQL("INSERT OR IGNORE INTO "+TABLE_PLAYCOUNTS
                +"(type, type_id, playcount, skipcount) VALUES ("+MediaUtils.TYPE_SONG+", "+id+", 0, 0);");
        //Tăng số lượt play lên
        dbh.execSQL("UPDATE "+TABLE_PLAYCOUNTS+" SET "+column+"="+column+"+1 WHERE type="+MediaUtils.TYPE_SONG+" AND type_id="+id+";");
        dbh.close();

        performGC(MediaUtils.TYPE_SONG);
    }

    /**
     * Trả về danh sách các bài hát được chơi nhiều nhất với số lượng limit
     * theo ids
     * @param limit
     * @return
     */
    public ArrayList<Long> getTopSongs(int limit){
        Log.d("TestPlayCounts","getTopSongs");
        ArrayList<Long> payload = new ArrayList<Long>();
        SQLiteDatabase dbh = getReadableDatabase();

        Cursor cursor = dbh.rawQuery("SELECT type_id FROM "+TABLE_PLAYCOUNTS+" WHERE type="
                +MediaUtils.TYPE_SONG+" AND playcount != 0 ORDER BY playcount DESC limit "+limit, null);

        while (cursor.moveToNext()) {
            payload.add(cursor.getLong(0));
            Log.d("TestPlayCounts",""+cursor.getLong(0));
        }

        cursor.close();
        dbh.close();
        return payload;
    }

    /**
     * Chọn ngẫu nhiên một vài bài hát từ dbh được truyền vào
     * sau đó sẽ kiểm tra với Androids media database.
     * nếu bài hát ko được tìm thấy trong media library thì sẽ bị removed từ DBH's database
     *
     * bài hát ko còn trong Androids media database có thể xảy ra khi t xóa bài hát từ bộ nhớ mà
     * DBH's database đã có thông tin về bài hát đó
     * @param type
     * @return
     */
    private int performGC(int type){
        SQLiteDatabase dbh = getWritableDatabase();
        ArrayList<Long> toCheck = new ArrayList<Long>();//danh sách các bài hát dùng để check
        QueryTask query;
        Cursor cursor;
        int removed = 0;

        //Lấy 10 đói tượng ngẫu nhiên xong đem đi check
        cursor = dbh.rawQuery("SELECT type_id FROM "+TABLE_PLAYCOUNTS+" WHERE type="+type+" ORDER BY RANDOM() LIMIT 10", null);
        while (cursor.moveToNext()) {
            toCheck.add(cursor.getLong(0));
        }
        cursor.close();

        for (Long id : toCheck) {
            query = MediaUtils.buildQuery(type, id, null, null);
            cursor = query.runQuery(ctx.getContentResolver());
            if(cursor.getCount() == 0) {
                dbh.execSQL("DELETE FROM "+TABLE_PLAYCOUNTS+" WHERE type="+type+" AND type_id="+id);
                removed++;
            }
            cursor.close();
        }

        dbh.close();
        return removed;
    }
}
