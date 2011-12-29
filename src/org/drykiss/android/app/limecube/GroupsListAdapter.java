
package org.drykiss.android.app.limecube;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.Group;

public class GroupsListAdapter extends ContactsListActivity.SelectableAdapterBase {
    private static final String TAG = "LimeCube_ContactsListAdapter";

    private LayoutInflater mLayoutInflater = null;
    private Context mContext = null;

    public GroupsListAdapter(Context context) {
        super();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return DataManager.getInstance().getGroupsCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View groupListItem;
        GroupListItemViewHolder holder;
        if (convertView == null) {
            groupListItem = mLayoutInflater.inflate(
                    R.layout.group_list_item, null, false);
            holder = new GroupListItemViewHolder();
            holder.mAccountType = (TextView) groupListItem
                    .findViewById(R.id.groupAccountTypeTextView);
            holder.mAccountName = (TextView) groupListItem
                    .findViewById(R.id.groupAccountNameTextView);
            holder.mTitle = (TextView) groupListItem.findViewById(R.id.groupTitleTextView);
            holder.mMemberCount = (TextView) groupListItem
                    .findViewById(R.id.groupMemberCountTextView);
            holder.mCheckBox = (CheckBox) groupListItem.findViewById(R.id.groupSelectedCheckBox);
            holder.mAccountsHeader = (LinearLayout) groupListItem
                    .findViewById(R.id.groupAccountLinearLayout);

            groupListItem.setTag(holder);

            holder.mCheckBox
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            View parent = (View) buttonView.getParent();
                            parent = (View) parent.getParent();

                            final GroupListItemViewHolder holder = (GroupListItemViewHolder) parent
                                    .getTag();
                            setSelected(holder.mPosition, isChecked);
                        }
                    });
            groupListItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("sj38.park_test", "got cha!!!");
                    final GroupListItemViewHolder holder = (GroupListItemViewHolder) v.getTag();
                    notifyItemClicked(holder.mPosition);
                }
            });
        } else {
            groupListItem = convertView;
            holder = (GroupListItemViewHolder) groupListItem.getTag();
        }

        final Group group = DataManager.getInstance().getGroup(position);

        holder.mPosition = position;

        boolean headerVisible = true;
        if (position > 0) {
            final Group previousGroup = DataManager.getInstance().getGroup(position - 1);
            headerVisible = !previousGroup.mAccountName.equals(group.mAccountName)
                    || !previousGroup.mAccountType.equals(group.mAccountType);
        }
        if (headerVisible) {
            holder.mAccountsHeader.setVisibility(View.VISIBLE);
            holder.mAccountType.setText(group.mAccountType);
            holder.mAccountName.setText(group.mAccountName);
        } else {
            holder.mAccountsHeader.setVisibility(View.GONE);
        }

        holder.mTitle.setText(group.mTitle);
        holder.mMemberCount.setText(mContext.getString(R.string.group_member_count,
                group.mMemberCount));
        holder.mCheckBox.setChecked(isSelected(position));

        return groupListItem;
    }

    @Override
    public Object getItem(int position) {
        return DataManager.getInstance().getGroup(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class GroupListItemViewHolder {
        int mPosition;
        CheckBox mCheckBox;
        TextView mAccountType;
        TextView mAccountName;
        TextView mTitle;
        TextView mMemberCount;
        LinearLayout mAccountsHeader;
    }
}
