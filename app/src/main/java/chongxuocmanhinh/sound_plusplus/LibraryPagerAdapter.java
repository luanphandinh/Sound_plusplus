package chongxuocmanhinh.sound_plusplus;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.widget.ListView;

/**
 * Created by L on 07/11/2016.
 */

/**
 * PagerAdapter that manages the library media listviews
 */
public class LibraryPagerAdapter extends PagerAdapter{
    /**
     *The number of unique list types.The number of visible lists may be smaller
     */
    public static final int MAX_ADAPTER_COUNT = 6;

    /**
     *The human-readable title for each list.The positions correspond to
     *the MediaUtls ids,eo e.g.TITLES[MediaUtils.TYPE_SONG] = R.string.songs
     */
    public static final int[] TITLES = { R.string.artists, R.string.albums, R.string.songs,
            R.string.playlists, R.string.genres, R.string.files };

    /**
     * Default tab order.
     */
    public static final int[] DEFAULT_ORDER = { MediaUtils.TYPE_ARTIST, MediaUtils.TYPE_ALBUM, MediaUtils.TYPE_SONG,
            MediaUtils.TYPE_PLAYLIST, MediaUtils.TYPE_GENRE, MediaUtils.TYPE_FILE };
    /**
     * The user-choosen tab order
     */
    int[] mTabOrder;
    /**
     * The number of visible Tabs.
     */
    private int mTabCount;

    /**
     *The listView for each adapter.Each index corresponds to that list's
     * MediaUtils's id.
     */
    private final ListView[] mLists = new ListView[MAX_ADAPTER_COUNT];

    /**
     *
     */





    /**
     *
     * @return
     */
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return false;
    }
}
