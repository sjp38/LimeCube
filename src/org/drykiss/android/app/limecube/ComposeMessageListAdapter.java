
package org.drykiss.android.app.limecube;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.SimpleContact;
import org.drykiss.android.app.limecube.widget.AbstractCheckableAdapter;
import org.drykiss.android.app.limecube.widget.SmsEditWidget;

import java.util.ArrayList;

public class ComposeMessageListAdapter extends AbstractCheckableAdapter {
    private Context mContext;
    private ArrayList<ComposeMessageItem> mItems = new ArrayList<ComposeMessageItem>();
    private Integer[] mTargets;
    private int mSuggestionWaiting;
    private int mLastFocusedInputText = -1;

    CompoundButton.OnCheckedChangeListener mContactCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            View parent = (View) buttonView.getParent();
            parent = (View) parent.getParent();
            final ComposeMessageItemViewHolder viewHolder = (ComposeMessageItemViewHolder) parent
                    .getTag();
            setSelected(viewHolder.mPosition, isChecked);
        }
    };
    RadioGroup.OnCheckedChangeListener mAddressCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            final View parent = (View) group.getParent();
            final ComposeMessageItemViewHolder viewHolder = (ComposeMessageItemViewHolder) parent
                    .getTag();
            final ComposeMessageItem item = mItems.get(viewHolder.mPosition);
            if (checkedId >= 0) {
                item.mSelectedAddress = checkedId;
            }
        }
    };

    private View.OnClickListener mOnSendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View parent = (View) v.getParent();
            parent = (View) parent.getParent();
            parent = (View) parent.getParent();
            parent = (View) parent.getParent();
            final ComposeMessageItemViewHolder viewHolder = (ComposeMessageItemViewHolder) parent
                    .getTag();
            final ComposeMessageItem item = mItems.get(viewHolder.mPosition);
            String number = item.mPhoneNumbers.get(item.mSelectedAddress);
            String body = item.mMessage;
            Uri uri = Uri.parse("sms:" + number);
            Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);
            sendIntent.putExtra("sms_body", body);
            ((Activity) mContext).startActivityForResult(sendIntent,
                    ComposeMessageActivity.REQUEST_SEND_SMS);

            final SimpleContact contact = item.mContact;
            DataManager.getInstance().addSuggestion(body, contact);
        }
    };

    private View.OnClickListener mOnMenuButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View parent = (View) v.getParent();
            parent = (View) parent.getParent();
            parent = (View) parent.getParent();
            parent = (View) parent.getParent();
            final ComposeMessageItemViewHolder viewHolder = (ComposeMessageItemViewHolder) parent
                    .getTag();

            mSuggestionWaiting = viewHolder.mPosition;
            Intent intent = new Intent();
            intent.setClassName("org.drykiss.android.app.limecube",
                    "org.drykiss.android.app.limecube.SuggestionsActivity");
            intent.putExtra(SuggestionsActivity.CONTACT_POSITION_EXTRA,
                    mTargets[viewHolder.mPosition]);
            ((Activity) mContext).startActivityForResult(intent,
                    ComposeMessageActivity.REQUEST_GET_SUGGESTION);
        }
    };

    @SuppressWarnings("unchecked")
    public ComposeMessageListAdapter(Context context, Integer[] targets) {
        super();
        mContext = context;
        mTargets = targets;

        for (Integer target : targets) {
            SimpleContact contact = DataManager.getInstance().getContact(target);
            mItems.add(new ComposeMessageItem(contact));
        }
        new NumberLoaderTask().execute(mItems);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ComposeMessageItemViewHolder viewHolder = null;
        if (convertView == null) {
            final LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.compose_message_item, null, false);
            final CheckBox checkBox = (CheckBox) convertView
                    .findViewById(R.id.compose_message_checkBox);
            final QuickContactBadge badge = (QuickContactBadge) convertView
                    .findViewById(R.id.compose_message_quickContactBadge);
            final TextView name = (TextView) convertView.findViewById(R.id.compose_message_to_name);
            final RadioGroup addressRadioGroup = (RadioGroup) convertView
                    .findViewById(R.id.compose_message_addressRadioGroup);
            final SmsEditWidget smsEditWidget = (SmsEditWidget) convertView
                    .findViewById(R.id.compose_message_to_textEditWidget);

            checkBox.setOnCheckedChangeListener(mContactCheckedChangedListener);
            addressRadioGroup.setOnCheckedChangeListener(mAddressCheckedChangedListener);
            smsEditWidget.setSendButtonListener(mOnSendButtonClickListener);
            smsEditWidget.setMenuButtonListener(mOnMenuButtonClickListener);

            smsEditWidget
                    .setOnEditTextFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            View parent = (View) v.getParent();
                            parent = (View) parent.getParent();
                            parent = (View) parent.getParent();
                            final ComposeMessageItemViewHolder holder = (ComposeMessageItemViewHolder) parent
                                    .getTag();
                            if (hasFocus) {
                                mLastFocusedInputText = holder.mPosition;
                            }
                        }
                    });

            viewHolder = new ComposeMessageItemViewHolder(checkBox, badge, name, addressRadioGroup,
                    smsEditWidget);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ComposeMessageItemViewHolder) convertView.getTag();
        }

        viewHolder.mPosition = position;

        final ComposeMessageItem item = mItems.get(position);
        final SimpleContact contact = item.mContact;
        viewHolder.mPosition = position;

        viewHolder.mCheckBox.setChecked(isSelected(position));

        final QuickContactBadge quickContact = viewHolder.mQuickContactBadge;
        quickContact.assignContactUri(Contacts.getLookupUri(contact.mId, contact.mLookupKey));
        final byte[] data = DataManager.getInstance()
                .getContactPhoto(contact.mId, contact.mPhotoId);
        if (data != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                quickContact.setImageBitmap(bitmap);
            } catch (OutOfMemoryError e) {
                quickContact.setImageResource(R.drawable.ic_contact_picture_holo_dark);
                // Photo will appear to be missing
            }
        } else {
            quickContact.setImageResource(R.drawable.ic_contact_picture_holo_dark);
        }

        viewHolder.mNameTextView.setText(contact.mName);

        final RadioGroup addressRadioGroup = viewHolder.mAddressRadioGroup;
        final ArrayList<String> numbers = item.mPhoneNumbers;
        final int radioGroupChildCount = addressRadioGroup.getChildCount();
        final int numbersCount = numbers.size();

        if (radioGroupChildCount < numbersCount) {
            for (int i = radioGroupChildCount; i < numbersCount; i++) {
                RadioButton radioButton = new RadioButton(mContext);
                radioButton.setId(i);
                LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                addressRadioGroup.addView(radioButton, -1, layoutParams);
            }
        }

        for (int i = 0; i < numbers.size(); i++) {
            final String number = numbers.get(i);
            if (number != null && !"".equals(number)) {
                RadioButton radioButton = (RadioButton) addressRadioGroup.getChildAt(i);
                radioButton.setText(number);
                radioButton.setVisibility(View.VISIBLE);
            }
        }

        for (int i = numbers.size(); i < addressRadioGroup.getChildCount(); i++) {
            View radioButton = addressRadioGroup.getChildAt(i);
            radioButton.setVisibility(View.GONE);
        }
        addressRadioGroup.clearCheck();
        addressRadioGroup.check(item.mSelectedAddress);

        viewHolder.mSmsEditWidget.setText(item.mMessage);

        if (position == mLastFocusedInputText) {
            viewHolder.mSmsEditWidget.setEditTextFocus();
        }
        return convertView;
    }

    public void onSuggestionSelected() {
        for (int i = mSuggestionWaiting; i < mItems.size(); i++) {
            ComposeMessageItem item = mItems.get(i);
            item.mMessage = DataManager.getInstance().getSuggestion(0, item.mContact);
        }
        notifyDataSetChanged();
    }

    private class ComposeMessageItemViewHolder {
        public ComposeMessageItemViewHolder(CheckBox checkBox, QuickContactBadge badge,
                TextView textView, RadioGroup radioGroup, SmsEditWidget smsEditWidget) {
            mCheckBox = checkBox;
            mQuickContactBadge = badge;
            mNameTextView = textView;
            mAddressRadioGroup = radioGroup;
            mSmsEditWidget = smsEditWidget;
        }

        int mPosition;
        CheckBox mCheckBox;
        QuickContactBadge mQuickContactBadge;
        TextView mNameTextView;
        RadioGroup mAddressRadioGroup;
        SmsEditWidget mSmsEditWidget;
    }

    public static class ComposeMessageItem {
        public ComposeMessageItem(SimpleContact contact) {
            mContact = contact;
            mMessage = DataManager.getInstance().getSuggestion(0, contact);
        }

        SimpleContact mContact;
        ArrayList<String> mPhoneNumbers = new ArrayList<String>();
        int mSelectedAddress = 0;
        String mMessage;
    }

    private class NumberLoaderTask extends AsyncTask<ArrayList<ComposeMessageItem>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<ComposeMessageItem>... params) {
            ArrayList<ComposeMessageItem> items = params[0];
            for (ComposeMessageItem item : items) {
                final SimpleContact contact = item.mContact;

                final Cursor phones = mContext.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contact.mId,
                        null, null);
                final ArrayList<String> numbers = new ArrayList<String>();
                while (phones.moveToNext()) {
                    numbers.add(phones.getString(phones
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                }
                phones.close();
                item.mPhoneNumbers = numbers;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            notifyDataSetChanged();
        }
    }
}
