package chongxuocmanhinh.sound_plusplus;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.view.KeyEvent;

/**
 * Created by L on 24/12/2016.
 */
@TargetApi(21)
public class RemoteControlImplLp implements RemoteControl.Client{
    /**
     * Context of this instance
     */
    private Context mContext;
    /**
     * Objects MediaSession handle
     */
    private MediaSession mMediaSession;
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
    public RemoteControlImplLp(Context context) {
        mContext = context;
    }

    /**
     * Thực hiện việc khởi tạo cho  RemoteControlClient.
     *
     */
    @Override
    public void initializeRemote() {
        //bỏ hết remote
        unregisterRemote();

        mMediaSession = new MediaSession(mContext, "Vanilla Music");

        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPause() {
                MediaButtonReceiver.processKey(mContext, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            }
            public void onPlay() {
                MediaButtonReceiver.processKey(mContext, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            }
            @Override
            public void onSkipToNext() {
                MediaButtonReceiver.processKey(mContext, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
            }
            public void onSkipToPrevious() {
                MediaButtonReceiver.processKey(mContext, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
            }
        });

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mContext.getPackageName(), MediaButtonReceiver.class.getName()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        // This Seems to overwrite our MEDIA_BUTTON intent filter and there seems to be no way to unregister it
        // Well: We intent to keep this around as long as possible anyway. But WHY ANDROID?!
        mMediaSession.setMediaButtonReceiver(pendingIntent);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
    }

    @Override
    public void unregisterRemote() {
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.release();
            mMediaSession = null;
        }
    }

    @Override
    public void reloadPreference() {

    }

    @Override
    public void updateRemote(Song song, int state, boolean keepPaused) {
        MediaSession session = mMediaSession;
        if (session == null)
            return;

        boolean isPlaying = ((state & PlaybackService.FLAG_PLAYING) != 0);

        if (mShowCover == -1) {
            SharedPreferences settings = PlaybackService.getSettings(mContext);
            mShowCover = settings.getBoolean(PrefKeys.COVER_ON_LOCKSCREEN, PrefDefaults.COVER_ON_LOCKSCREEN) ? 1 : 0;
        }

        if (song != null) {
            Bitmap bitmap = null;
            if (mShowCover == 1 && (isPlaying || keepPaused)) {
                bitmap = song.getCover(mContext);
            }

            session.setMetadata(new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                    .build());
        }

        int playbackState = (isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);

        session.setPlaybackState(new PlaybackState.Builder()
                .setState(playbackState, PlaybackState.PLAYBACK_POSITION_UNKNOWN , 1.0f)
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .build());
        mMediaSession.setActive(true);
    }
}
