
package org.drykiss.android.app.limecube.widget;

import android.widget.BaseAdapter;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class AbstractCheckableAdapter extends BaseAdapter {
    private SortedSet<Integer> mSelectedItems = new TreeSet<Integer>();
    private OnItemClickedListener mListener = null;

    protected AbstractCheckableAdapter() {
        super();
    }

    public boolean isSelected(int position) {
        return mSelectedItems.contains(position);
    }

    public SortedSet<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public void setSelected(int position, boolean selected) {
        if (selected) {
            mSelectedItems.add(position);
        } else {
            mSelectedItems.remove(position);
        }
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean all) {
        mSelectedItems.clear();
        if (all) {
            for (int i = 0; i < getCount(); i++) {
                mSelectedItems.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    protected void notifyItemClicked(int position) {
        mListener.onItemClicked(position);
    }

    public interface OnItemClickedListener {
        public void onItemClicked(int position);
    }
}
