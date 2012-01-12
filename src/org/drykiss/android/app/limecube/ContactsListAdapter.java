
package org.drykiss.android.app.limecube;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.SimpleContact;
import org.drykiss.android.app.limecube.widget.AbstractCheckableAdapter;

public class ContactsListAdapter extends AbstractCheckableAdapter {
    private static final String TAG = "LimeCube_ContactsListAdapter";

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
                    setSelected(holder.mPosition, checkBox.isChecked());
                }
            });
        } else {
            contactListItem = convertView;
            holder = (ContactListItemViewHolder) contactListItem.getTag();
        }

        final SimpleContact contact = DataManager.getInstance().getContact(
                position);

        holder.mPosition = position;

        holder.mCheckBox.setChecked(isSelected(position));
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
