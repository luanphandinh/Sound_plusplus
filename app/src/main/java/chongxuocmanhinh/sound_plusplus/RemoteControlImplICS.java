package chongxuocmanhinh.sound_plusplus;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.util.Log;


/**
 * Created by L on 24/12/2016.
 */
public class RemoteControlImplICS implements RemoteControl.Client {
    /**
     * Context of this instance
     */
    private Context mContext;
    /**
     * Used with updateRemote method.
     */
    private RemoteControlClient mRemote;
    /**
     * Whether the cover should be shown. 1 for yes, 0 for no, -1 for
     * uninitialized.
     */
    private int mShowCover = -1;

    /**
     * Creates a new instance
     *
     * @param context The context to use
     */
    public RemoteControlImplICS(Context context) {
        mContext = context;
    }

    @Override
    public void initializeRemote() {
        Log.d("TestRemote","Inited Remote");
        //Bảo đảm chỉ có duy nhất một remote được registered
        unregisterRemote();

        //Nhận 'background' play button events
        AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        ComponentName receiver = new ComponentName(mContext.getPackageName(), MediaButtonReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(receiver);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(new ComponentName(mContext.getPackageName(), MediaButtonReceiver.class.getName()));
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(mContext, 0, mediaButtonIntent, 0);
        RemoteControlClient remote = new RemoteControlClient(mediaPendingIntent);

        // Things we can do (eg: buttons to display on lock screen)
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE;
        remote.setTransportControlFlags(flags);

        audioManager.registerRemoteControlClient(remote);
        mRemote = remote;
    }

    @Override
    public void unregisterRemote() {
        if (mRemote != null) {
            AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
            ComponentName receiver = new ComponentName(mContext.getPackageName(), MediaButtonReceiver.class.getName());
            audioManager.unregisterMediaButtonEventReceiver(receiver);
            audioManager.unregisterRemoteControlClient(mRemote);
            mRemote = null;
        }
    }

    @Override
    public void reloadPreference() {
            mShowCover = -1;
    }

    @Override
    public void updateRemote(Song song, int state, boolean keepPaused) {
        Log.d("TestRemote","update Remote");
        RemoteControlClient remote = mRemote;
        if(remote == null)
            return;

        boolean isPlaying = ((state & PlaybackService.FLAG_PLAYING) != 0);

        remote.setPlaybackState(isPlaying ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
        RemoteControlClient.MetadataEditor editor = remote.editMetadata(true);
        if (song != null && song.artist != null && song.album != null) {
            String artist_album = song.artist + " - " + song.album;
            artist_album = (song.artist.length() == 0 ? song.album : artist_album); // no artist ? -> only display album
            artist_album = (song.album.length() == 0 ? song.artist : artist_album); // no album ? -> only display artist

            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, artist_album);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.title);
        }
        editor.apply();
    }
}
