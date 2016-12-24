package chongxuocmanhinh.sound_plusplus;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AsyncPlayer;
import android.net.Uri;
import android.view.KeyEvent;

/**
 * Created by L on 24/12/2016.
 */
public class MediaButtonReceiver extends BroadcastReceiver{

    /**
     * Truyền action qua cho service
     * @param context
     * @param act
     */
    public static void runAction(Context context,String act){
        if(act == null)
            return;

        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(act);
        context.startService(intent);
    }

    public static boolean processKey(Context context, KeyEvent event) {
        if (event == null)
            return false;

        int action = event.getAction();
        String act = null;

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (action == KeyEvent.ACTION_DOWN)
                    act = PlaybackService.ACTION_TOGGLE_PLAYBACK;
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (action == KeyEvent.ACTION_DOWN)
                    act = PlaybackService.ACTION_NEXT_SONG_AUTOPLAY;
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (action == KeyEvent.ACTION_DOWN)
                    act = PlaybackService.ACTION_PREVIOUS_SONG_AUTOPLAY;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (action == KeyEvent.ACTION_DOWN)
                    act = PlaybackService.ACTION_PLAY;
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (action == KeyEvent.ACTION_DOWN)
                    act = PlaybackService.ACTION_PAUSE;
                break;
            default:
                return false;
        }
        runAction(context,act);
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())){
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            boolean handled = processKey(context,keyEvent);
            if(handled && isOrderedBroadcast())
                abortBroadcast();
        }
    }


}