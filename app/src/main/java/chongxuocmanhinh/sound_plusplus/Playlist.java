package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by lordhung on 27/12/2016.
 * Cung cấp nhiều hàm liên quan xử lý playlist
 */

public class Playlist {
    /**
     * Delete playlist theo id
     *
     * @param resolver ContentResolver
     * @param id Media.Audio.Playlists id của playlist cần xóa
     * */
    public static void deletePlaylist(ContentResolver resolver,long id){
        Uri uri= ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,id);
        resolver.delete(uri,null,null);
    }
}
