package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.os.Build;

/**
 * Created by L on 24/12/2016.
 */
public class RemoteControl {
    /**
     * Trả về RemoteControl.Client implementation
     */
    public RemoteControl.Client getClient(Context context){
//        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
//                new RemoteControlImplLp(context) :
          return      new RemoteControlImplICS(context);//Dùng cho android 4.x trở xuống
//        );
    }

    /**
     * Interface definition of our RemoteControl API
     */
    public interface Client {
        public void initializeRemote();
        public void unregisterRemote();
        public void reloadPreference();
        public void updateRemote(Song song, int state, boolean keepPaused);
    }
}
