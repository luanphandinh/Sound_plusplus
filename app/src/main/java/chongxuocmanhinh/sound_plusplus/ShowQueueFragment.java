package chongxuocmanhinh.sound_plusplus;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by L on 05/12/2016.
 */
public class ShowQueueFragment extends Fragment
        implements  TimelineCallback,
                    AdapterView.OnItemClickListener
{
    //Sau này phần này sẽ chuyển sang draggable row nếu có thể
    private ListView mListView;
    private ShowQueueAdapter mListAdapter;
    private PlaybackService mService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.showqueue_listview, container, false);
        Context context = getActivity();

        mListView    = (ListView) view.findViewById(R.id.list);
        mListAdapter = new ShowQueueAdapter(context, R.layout.draggable_row);
        mListView.setAdapter(mListAdapter);
        PlaybackService.addTimelineCallback(this);
        return view;
    }

    //===========================AdapterView.OnItemClickListener=====================//
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }


    /**
     * làm mới danh sách nhạc
     * @param scroll
     */
    public void refreshSongQueueList(final boolean scroll){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Testtt","refresh");
                int i,stotal,spos;
                stotal = mService.getTimeLineLength();
                spos = mService.getTimelinePosition();

                mListAdapter.clear();
                mListAdapter.highlightRow(spos);

                for(i = 0;i <stotal; i++){
                    mListAdapter.add(mService.getSongByQueuePosition(i));
                }
            }
        });
    }
    //=========================TimeLineCallBack==========================//
    @Override
    public void setSong(long uptime, Song song) {
        Log.d("Testtt","fragment setSong");
        if (mService == null) {
            mService = PlaybackService.get(getActivity());
            onTimelineChanged();
        }
    }

    @Override
    public void onTimelineChanged() {
        Log.d("Testtt","fragment onTimelineChanged");
        if (mService != null)
            refreshSongQueueList(false);
    }
}
