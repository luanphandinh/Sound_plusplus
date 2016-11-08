package chongxuocmanhinh.sound_plusplus;

import android.content.Intent;
import android.view.View;
import android.widget.ListAdapter;

/**
 * Created by L on 08/11/2016.
 */

/**
 * Provides support for limitters and a few other methods LibraryActivity uses
 * for its adapters.
 */
public interface LibraryAdapter extends ListAdapter {
    /**
     *Return the type of media represented by this adapter.One of
     * MediaUtls.TYPE_*.
     */
    int getMediaTypes();

    /**
     * Set the limiter for the adapter.
     *
     * A limiter is intended to restrict displayed media to only those that are
     * children of a given parent media item.
     *
     * @param limiter The limiter,created by
     *                {@link LibraryAdapter#buildLimiter(long)}.
     */
    void setLimiter(Limiter limiter);

    /**
     * Return the limiter currently active on this adapter or null if none are active
     * @return
     */
    Limiter getLimiter();

    /**
     * Build a limiter base off of the media represented by the give row.
     *
     * @param id The id of the row
     * @see LibraryAdapter#getLimiter()
     * @see LibraryAdapter#setLimiter(Limiter)
     */
    Limiter buildLimiter(long id);

    /**
     * Set a new filter.
     *
     * The data should be requeried after calling this
     *
     * @param filters the term to filter on,separate by space.Only
     *               media that contain all of the terms(in any order) will
     *                be displayed after filtering is completed
     */
    void setFilters(String filters);

    /**
     * Retrieve the data for this adapter.the data must be set with
     * {@link LibraryAdapter#commitQuery(Object)} before it takes effect.
     *
     * This should be called on a worker thread.
     *
     * @return the data.Contents depend on the sub-class
     */
    Object query();

    /**
     * Update the adapter with the given data
     *
     * Must be called on the UI Thread.
     *
     * @param data Data from {@link LibraryAdapter#query()}.
     */
    void commitQuery(Object data);

    /**
     * Clear the data for this adapter.
     *
     * Must be called on the UI thread.
     */
    void clear();

    /**
     * Creates the row data used by LibraryActivity.
     * @param row
     * @return
     */
    Intent createData(View row);

    /**
     * Extra for row data: media id. type: long.
     */
    String DATA_ID = "id";
    /**
     * Special id for {@link #DATA_ID}: the row represented is a header view.
     */
    long HEADER_ID = -1;
    /**
     * Special id for {@link #DATA_ID}: invalid id.
     */
    long INVALID_ID = -2;
    /**
     * Extra for row data: media title. type: String.
     */
    String DATA_TITLE = "title";
    /**
     * Extra for row data: media type. type: int. One of MediaUtils.TYPE_*.
     */
    String DATA_TYPE = "type";
    /**
     * Extra for row data: canonical file path. type: String. Only present if
     * type is {@link MediaUtils#TYPE_FILE}.
     */
    String DATA_FILE = "file";
    /**
     * Extra for row data: if true, row has expander arrow. type: boolean.
     */
    String DATA_EXPANDABLE = "expandable";
}
