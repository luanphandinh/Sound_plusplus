package chongxuocmanhinh.sound_plusplus;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

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

    /**
     * Query tất cả playlist trong MediaStore
     *
     * @param resolver ContentResolver
     * @return queries cursor
     * */
    public static Cursor queryPlaylists(ContentResolver resolver){
        Uri media= MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection={
                MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME
        };
        // sort theo tên
        String sort= MediaStore.Audio.Playlists.NAME;
        return MediaUtils.queryResolver(resolver,media,projection,null,null,null);
    }

    /**
     * Lấy id của một playlist theo tên
     * */
    public static long getPlaylist(ContentResolver resolver,String name){
        long id=-1;

        Cursor cursor=MediaUtils.queryResolver(resolver,MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Playlists._ID},
                MediaStore.Audio.Playlists.NAME+"=?",
                new String[] {name},null);

        if(cursor!=null){
            if(cursor.moveToNext())
                id=cursor.getLong(0);
            cursor.close();
        }

        return id;
    }

    /**
     * Tạo 1 playlist với tên dc cho. Nếu playlist với tên đó đã tồn tại, nó sẽ bị overwrite
     *
     * @param resolver ContentResolver.
     * @param name Tên playlis .
     * @return id của playlist mới .
     */
    public static long createPlaylist(ContentResolver resolver, String name)
    {
        long id = getPlaylist(resolver, name);

        if (id == -1) {
            // Tạo new playlist .
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Audio.Playlists.NAME, name);
            Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
			/*
			 * Tạo playlist có thể fail vì race condition hoặc android bugs. Trường hợp này thì id sẽ là -1
			 */
            if (uri != null) {
                id = Long.parseLong(uri.getLastPathSegment());
            }
        } else {
            // Overwrite một playlist đã tồn tại, clear những bài hát đã tồn tại.
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);
            resolver.delete(uri, null, null);
        }

        return id;
    }

    /**
     * Chạy query dc cho và thêm result vào playlist đã cho. Hàm này nên chạy trên 1 thread background
     *
     * @param resolver ContentResolver.
     * @param playlistId MediaStore.Audio.Playlist id cửa playlist cần modify
     * @param query query dc dùng, audioId nên là cột đầu tiên.
     * @return số lượng bài hát  dc thêm vào playlist .
     */
    public static int addToPlaylist(ContentResolver resolver, long playlistId, QueryTask query) {
        ArrayList<Long> result = new ArrayList<Long>();
        Cursor cursor = query.runQuery(resolver);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(0));
            }
        }
        return addToPlaylist(resolver, playlistId, result);
    }

    /**
     * Thêm 1 tập hợp các audioIds tới playlist. Nên chạy trên background thread
     *
     * @param resolver ContentResolver.
     * @param playlistId MediaStore.Audio.Playlist id của playlist cần modify
     * @param audioIds một ArrayList chứa tất cả ids cần thêm
     * @return số lượng bài hát đã dc thêm vào playlist .
     */
    public static int addToPlaylist(ContentResolver resolver, long playlistId, ArrayList<Long> audioIds) {
        if (playlistId == -1)
            return 0;

        // tìm PLAY_ODER lớn nhất trong playlist
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        String[] projection = new String[] { MediaStore.Audio.Playlists.Members.PLAY_ORDER };
        Cursor cursor = MediaUtils.queryResolver(resolver, uri, projection, null, null, null);
        int base = 0;
        if (cursor.moveToLast())
            base = cursor.getInt(0) + 1;
        cursor.close();

        int count = audioIds.size();
        if (count > 0) {
            ContentValues[] values = new ContentValues[count];
            for (int i = 0; i != count; ++i) {
                ContentValues value = new ContentValues(2);
                value.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + i));
                value.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioIds.get(i));
                values[i] = value;
            }
            resolver.bulkInsert(uri, values);
        }

        return count;
    }

    /**
     * Remove 1 tập hợp các audioIds của playlist. Nên chạy trên background thread
     *
     * @param resolver ContentResolver.
     * @param playlistId MediaStore.Audio.Playlist id của playlist cần modify
     * @param audioIds một ArrayList chứa tất cả ids cần remove
     * @return số lượng bài hát đã dc remove khỏi playlist .
     */
    public static int removeFromPlaylist(ContentResolver resolver, long playlistId, ArrayList<Long> audioIds) {
        if (playlistId == -1)
            return 0;

        int count = 0;
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        for (long id : audioIds) {
            String where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + id;
            count += resolver.delete(uri, where, null);
        }
        return count;
    }



    /**
     * Copy content từ 1 playlist tới playlist khác
     *
     * @param resolver ContentResolver.
     * @param sourceId Media.Audio.Playlists id của source playlist
     * @param destinationId Media.Audio.Playlists id của destination playlist
     */
    private static void _copyToPlaylist(ContentResolver resolver, long sourceId, long destinationId) {
        QueryTask query = MediaUtils.buildPlaylistQuery(sourceId, Song.FILLED_PLAYLIST_PROJECTION, null);
        addToPlaylist(resolver, destinationId, query);
    }

    /**
     * Rename playlist theo id
     *
     * @param resolver ContentResolver.
     * @param id Media.Audio.Playlists id của playlist.
     * @param newName Tên mới cho playlist.
     */
    public static void renamePlaylist(ContentResolver resolver, long id, String newName)
    {
        long newId = createPlaylist(resolver, newName);
        if (newId != -1) { // new playlist created -> move stuff over
            _copyToPlaylist(resolver, id, newId);
            deletePlaylist(resolver, id);
        }
    }

}
