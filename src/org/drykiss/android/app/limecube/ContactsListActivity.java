
package org.drykiss.android.app.limecube;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.ContactsManager.OnContactsDataChangedListener;
import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.SimpleContact;

import java.util.SortedSet;
import java.util.TreeSet;

public class ContactsListActivity extends ActionBarActivity {
    private static final String TAG = "lomeCube_contacts_list";
    public static final String CHECKED_CONTACTS_EXTRA_NAME = "checkedContacts";
    private ListView mContactsList = null;
    private ContactsListAdapter mAdapter = null;
    private SortedSet<Integer> mCheckedContacts = new TreeSet<Integer>();
    private CheckBox mSelectAllCheckBox;

    private View mAdView;

    private OnContactsDataChangedListener mListener = new OnContactsDataChangedListener() {
        @Override
        public void onContactsDataChanged() {
            mAdapter.notifyDataSetChanged();
        }
    };

    private OnCheckedChangeListener mSelectAllListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            mCheckedContacts.clear();
            if (isChecked) {
                for (int i = 0; i < DataManager.getInstance().getContactsCount(); i++) {
                    mCheckedContacts.add(i);
                }
                view.setText(R.string.clear_all_selection);
            } else {
                view.setText(R.string.select_all);
            }
            mAdapter.notifyDataSetChanged();
        }
    };

    /**
     * Called when fg the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list);

        DataManager.getInstance().setContext(getApplicationContext());
        DataManager.getInstance().startDataLoading();
        DataManager.getInstance().setOnContactsDataChangedListener(mListener);

        mSelectAllCheckBox = (CheckBox) findViewById(R.id.contactslistSelectAllCheckBox);
        mSelectAllCheckBox.setOnCheckedChangeListener(mSelectAllListener);

        mContactsList = (ListView) findViewById(R.id.contactslistView);
        mAdapter = new ContactsListAdapter(this);
        mContactsList.setAdapter(mAdapter);
        mContactsList.setClickable(true);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);
        DataManager.getInstance().stopDataLoading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.contacts_list_activity_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_send_message:
                if (mCheckedContacts.size() <= 0) {
                    Toast.makeText(
                            this,
                            R.string.warning_select_contact_first_to_send_messages,
                            Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }
                Intent intent = new Intent(this, ComposeMessageActivity.class);

                intent.putExtra(CHECKED_CONTACTS_EXTRA_NAME,
                        mCheckedContacts.toArray());
                mCheckedContacts.clear();
                mAdapter.notifyDataSetChanged();
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ContactsListAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater = null;

        public ContactsListAdapter(Context context) {
            super();
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return DataManager.getInstance().getContactsCount();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View contactListItem;
            ContactListItemViewHolder holder;
            if (convertView == null) {
                contactListItem = mLayoutInflater.inflate(
                        R.layout.contact_list_item, null, false);
                holder = new ContactListItemViewHolder();
                holder.mName = (TextView) contactListItem
                        .findViewById(R.id.contact_item_name);
                holder.mCheckBox = (CheckBox) contactListItem
                        .findViewById(R.id.contact_item_checkBox);
                holder.mQuickContact = (QuickContactBadge) contactListItem
                        .findViewById(R.id.contact_item_quickContactBadge);
                contactListItem.setTag(holder);
                contactListItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContactListItemViewHolder holder = (ContactListItemViewHolder) v
                                .getTag();
                        final CheckBox checkBox = holder.mCheckBox;
                        checkBox.setChecked(!checkBox.isChecked());
                        if (checkBox.isChecked()) {
                            mCheckedContacts.add(holder.mPosition);
                        } else {
                            mCheckedContacts.remove(holder.mPosition);
                        }
                    }
                });
            } else {
                contactListItem = convertView;
                holder = (ContactListItemViewHolder) contactListItem.getTag();
            }

            final SimpleContact contact = DataManager.getInstance().getContact(
                    position);

            holder.mPosition = position;

            holder.mCheckBox.setChecked(mCheckedContacts.contains(position));
            holder.mName.setText(contact.mName);

            final QuickContactBadge quickContact = holder.mQuickContact;
            quickContact.assignContactUri(Contacts.getLookupUri(contact.mId,
                    contact.mLookupKey));

            final byte[] data = contact.mPhoto;
            if (data != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                            data.length);
                    quickContact.setImageBitmap(bitmap);
                } catch (OutOfMemoryError e) {
                    Log.d(TAG, "OutOfMemory while decode photo");
                    quickContact
                            .setImageResource(R.drawable.ic_contact_picture_holo_dark);
                    // Photo will appear to be missing
                }
            } else {
                quickContact
                        .setImageResource(R.drawable.ic_contact_picture_holo_dark);
            }

            return contactListItem;
        }

        @Override
        public Object getItem(int position) {
            return DataManager.getInstance().getContact(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ContactListItemViewHolder {
            int mPosition;
            CheckBox mCheckBox;
            TextView mName;
            QuickContactBadge mQuickContact;
        }
    }
}
