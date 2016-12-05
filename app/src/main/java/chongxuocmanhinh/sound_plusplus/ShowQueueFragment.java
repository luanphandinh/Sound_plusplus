package chongxuocmanhinh.sound_plusplus;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by L on 05/12/2016.
 */
public class ShowQueueFragment extends Fragment
        implements AdapterView.OnItemClickListener
{
    //Sau này phần này sẽ chuyển sang draggable row nếu có thể
    private ListView mListView;
    private ShowQueueAdapter mListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.showqueue_listview, container, false);
        Context context = getActivity();

        mListView    = (ListView) view.findViewById(R.id.list);
        mListAdapter = new ShowQueueAdapter(context, R.layout.draggable_row);
        mListView.setAdapter(mListAdapter);
        return view;
    }

    //===========================AdapterView.OnItemClickListener=====================//
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
