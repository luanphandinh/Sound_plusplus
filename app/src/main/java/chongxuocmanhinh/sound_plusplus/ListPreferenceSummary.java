package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by L on 29/12/2016.
 */
public class ListPreferenceSummary extends ListPreference {
    public ListPreferenceSummary(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary()
    {
        return getEntry();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);
        notifyChanged();
    }
}
