
package org.drykiss.android.app.limecube;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.limecube.ContactsListActivity.SelectableAdapterBase.OnItemClickListener;
import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.ContactsManager.OnContactsDataChangedListener;
import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.Group;
import org.drykiss.android.app.limecube.data.GroupsManager.OnGroupsDataChangedListener;

import java.util.SortedSet;
import java.util.TreeSet;

public class ContactsListActivity extends ActionBarActivity {
    private static final String TAG = "limeCube_contacts_list";
    public static final String CHECKED_ITEMS_EXTRA_NAME = "checkedItems";
    private static final String EXTRA_MODE_NAME = "extra_mode";
    private static final String EXTRA_GROUP_ID = "extra_group_id";

    private static final int MODE_CONTACTS = 0;
    private static final int MODE_GROUPS = 1;

    private int mMode = MODE_CONTACTS;
    private long mGroupId = -1;
    private ListView mContactsList = null;
    private SelectableAdapterBase mAdapter = null;
    private CheckBox mSelectAllCheckBox;

    private View mAdView;

    private OnContactsDataChangedListener mListener = new OnContactsDataChangedListener() {
        @Override
        public void onContactsDataChanged() {
            if (mMode == MODE_CONTACTS) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private OnGroupsDataChangedListener mGroupsListener = new OnGroupsDataChangedListener() {
        @Override
        public void onGroupsDataChanged() {
            if (mMode == MODE_GROUPS) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private OnItemClickListener mGroupsListItemClickedListener = new OnItemClickListener() {
        @Override
        public void onItemClicked(int position) {
            Group group = DataManager.getInstance().getGroup(position);
            startNewMode(MODE_CONTACTS, group.mId);
        }
    };

    private OnCheckedChangeListener mSelectAllListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            mAdapter.setAllSelected(isChecked);
            if (isChecked) {
                view.setText(R.string.clear_all_selection);
            } else {
                view.setText(R.string.select_all);
            }
        }
    };

    /**
     * Called when fg the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list);

        final int mode = getIntent().getIntExtra(EXTRA_MODE_NAME, MODE_CONTACTS);
        mMode = mode;
        final long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, -1);
        mGroupId = groupId;

        mSelectAllCheckBox = (CheckBox) findViewById(R.id.contactslistSelectAllCheckBox);
        mSelectAllCheckBox.setOnCheckedChangeListener(mSelectAllListener);

        DataManager.getInstance().setContext(getApplicationContext());
        if (mode == MODE_GROUPS) {
            setTitle(R.string.groupsListLabel);
            mSelectAllCheckBox.setVisibility(View.GONE);
            DataManager.getInstance().startGroupsDataLoading();
            DataManager.getInstance().setOnGroupsDataChangedListener(mGroupsListener);
        } else {
            if (mGroupId >= 0) {
                setTitle(DataManager.getInstance().getGroupTitle(groupId));
            }
            DataManager.getInstance().startContactsLoading(groupId);
            DataManager.getInstance().setOnContactsDataChangedListener(mListener);
        }

        mContactsList = (ListView) findViewById(R.id.contactslistView);
        mAdapter = getListAdapter();
        if (mode == MODE_GROUPS) {
            mAdapter.setOnItemClickListener(mGroupsListItemClickedListener);
        }
        mContactsList.setAdapter(mAdapter);
        mContactsList.setClickable(true);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    SelectableAdapterBase getListAdapter() {
        if (mMode == MODE_GROUPS) {
            return new GroupsListAdapter(this);
        }
        return new ContactsListAdapter(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);
        if (mMode == MODE_GROUPS) {
            DataManager.getInstance().stopGroupsDataLoading();
        } else {
            DataManager.getInstance().stopContactsDataLoading();
        }
    }

    @Override
    public void onBackPressed() {
        if (mMode == MODE_GROUPS) {
            startNewMode(MODE_CONTACTS, -1);
        } else if (mMode == MODE_CONTACTS && mGroupId >= 0) {
            startNewMode(MODE_GROUPS, -1);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.contacts_list_activity_menu, menu);
        if (mMode == MODE_CONTACTS) {
            if (mGroupId == -1) {
                menu.removeItem(R.id.menu_action_contacts_list);
            }
        } else if (mMode == MODE_GROUPS) {
            menu.removeItem(R.id.menu_action_groups_list);
            menu.removeItem(R.id.menu_action_send_message);
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void startNewMode(int newMode, long groupId) {
        final Intent newIntent = new Intent(this, ContactsListActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.putExtra(EXTRA_MODE_NAME, newMode);
        newIntent.putExtra(EXTRA_GROUP_ID, groupId);
        startActivity(newIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_contacts_list:
                startNewMode(MODE_CONTACTS, -1);
                break;
            case R.id.menu_action_groups_list:
                startNewMode(MODE_GROUPS, -1);
                break;
            case R.id.menu_action_send_message:
                SortedSet<Integer> checkedItems = mAdapter.getSelectedItems();
                if (checkedItems.size() <= 0) {
                    Toast.makeText(
                            this,
                            R.string.warning_select_contact_first_to_send_messages,
                            Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }
                Intent intent = new Intent(this, ComposeMessageActivity.class);

                intent.putExtra(CHECKED_ITEMS_EXTRA_NAME, checkedItems.toArray());
                startActivity(intent);
                mAdapter.setAllSelected(false);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SelectableAdapterBase extends BaseAdapter {
        private SortedSet<Integer> mSelectedItems = new TreeSet<Integer>();
        private OnItemClickListener mListener = null;

        protected SelectableAdapterBase() {
            super();
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
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

        public void setOnItemClickListener(OnItemClickListener listener) {
            mListener = listener;
        }

        protected void notifyItemClicked(int position) {
            mListener.onItemClicked(position);
        }

        public interface OnItemClickListener {
            public void onItemClicked(int position);
        }
    }
}
