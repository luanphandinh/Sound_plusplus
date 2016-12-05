package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by L on 05/12/2016.
 */
public class ShowQueueAdapter extends ArrayAdapter<Song>{

    private int mResource;
    private int mHighlightRow;
    private Context mContext;

    public ShowQueueAdapter(Context context, int resource) {
        super(context, resource);
        mResource = resource;
        mContext  = context;
        mHighlightRow = -1;
    }

    /**
     * Tells the adapter to highlight a specific row id
     * Set this to -1 to disable the feature
     */
    public void highlightRow(int pos) {
        mHighlightRow = pos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DraggableRow row;

        if(convertView != null){
            row = (DraggableRow) convertView;
        }else{
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            row = (DraggableRow) inflater.inflate(mResource,parent,false);
            row.setupLayout(DraggableRow.LAYOUT_DRAGGABLE);
        }

        Song song = getItem(position);

        if (song != null) { // unlikely to fail but seems to happen in the wild.
            SpannableStringBuilder sb = new SpannableStringBuilder(song.title);
            sb.append('\n');
            sb.append(song.album+", "+song.artist);
            sb.setSpan(new ForegroundColorSpan(Color.GRAY), song.title.length() + 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            row.getTextView().setText(sb);
            //row.getCoverView().setCover(MediaUtils.TYPE_ALBUM, song.albumId, null);
        }

        row.highlightRow(position == mHighlightRow);

        return row;
    }
}
