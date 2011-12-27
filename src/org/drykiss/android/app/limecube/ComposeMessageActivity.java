
package org.drykiss.android.app.limecube;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.SimpleContact;
import org.drykiss.android.app.limecube.widget.SmsEditWidget;

import java.util.ArrayList;

public class ComposeMessageActivity extends ActionBarActivity {
    private static final String TAG = "limeCube_compose_message";
    private static final int REQUEST_SEND_SMS = 0;
    private static final int REQUEST_GET_SUGGESTION = 1;

    private Integer[] mTargetContacts;
    private int mCurrentContactIndex = 0;

    private QuickContactBadge mQuickContact;
    private TextView mName;
    private TextView mNumber;
    private SmsEditWidget mMessageEditor;

    private View mAdView;

    private View.OnClickListener onSendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String number = mNumber.getText().toString();
            String body = mMessageEditor.getText().toString();
            Uri uri = Uri.parse("sms:" + number);
            Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);
            sendIntent.putExtra("sms_body", body);
            startActivityForResult(sendIntent, REQUEST_SEND_SMS);

            final SimpleContact contact = DataManager.getInstance().getContact(
                    mTargetContacts[mCurrentContactIndex - 1]);
            DataManager.getInstance().addSuggestion(body, contact);

            bindViewsToNextTarget();
        }
    };

    private View.OnClickListener onMenuButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ComposeMessageActivity.this, SuggestionsActivity.class);
            intent.putExtra(SuggestionsActivity.CONTACT_POSITION_EXTRA,
                    mTargetContacts[mCurrentContactIndex - 1]);
            startActivityForResult(intent, REQUEST_GET_SUGGESTION);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_message);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        Object[] received = (Object[]) bundle
                .get(ContactsListActivity.CHECKED_CONTACTS_EXTRA_NAME);
        mTargetContacts = new Integer[received.length];
        for (int i = 0; i < received.length; i++) {
            mTargetContacts[i] = (Integer) received[i];
        }

        initView();

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    public void onDestroy() {
        AdvertisementManager.destroyAd(mAdView);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SEND_SMS:
                // Do nothing. Leave code for future.
                break;
            case REQUEST_GET_SUGGESTION:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Return from suggestions");
                    String suggestion = data.getStringExtra(SuggestionsActivity.SUGGESTION_EXTRA);
                    mMessageEditor.setText(suggestion);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.compose_message_activity_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_skip_this_number:
                bindViewsToNextTarget();
                break;
            case R.id.menu_action_compose_later:
                Integer[] targetContacts = new Integer[mTargetContacts.length];
                System.arraycopy(mTargetContacts, 0, targetContacts, 0, mCurrentContactIndex-1);
                System.arraycopy(mTargetContacts, mCurrentContactIndex, targetContacts, mCurrentContactIndex-1, mTargetContacts.length - mCurrentContactIndex);
                targetContacts[mTargetContacts.length-1] = mTargetContacts[mCurrentContactIndex-1];
                mTargetContacts = targetContacts;
                
                mCurrentContactIndex--;
                bindViewsToNextTarget();
                break;
            default:
                break;
        }
        return true;
    }

    private void findViews() {
        mQuickContact = (QuickContactBadge) findViewById(R.id.compose_message_quickContactBadge);
        mName = (TextView) findViewById(R.id.compose_message_to_name);
        mNumber = (TextView) findViewById(R.id.compose_message_to_number);
        mMessageEditor = (SmsEditWidget) findViewById(R.id.compose_message_to_textEditWidget);
    }

    private void bindViewsToNextTarget() {
        if (mCurrentContactIndex >= mTargetContacts.length) {
            finish();
            return;
        }
        SimpleContact contact = DataManager.getInstance().getContact(
                mTargetContacts[mCurrentContactIndex++]);

        final String suggestion = DataManager.getInstance().getSuggestion(0, contact);
        mMessageEditor.setText(suggestion);

        final QuickContactBadge quickContact = mQuickContact;
        quickContact.assignContactUri(Contacts.getLookupUri(contact.mId, contact.mLookupKey));

        final byte[] data = contact.mPhoto;
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

        mName.setText(contact.mName);

        final Cursor phones = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " +
                        contact.mId, null, null);
        final ArrayList<String> numbers = new
                ArrayList<String>();
        while (phones.moveToNext()) {
            numbers.add(phones.getString(phones
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
        }
        phones.close();

        if (numbers.size() == 1) {
            mNumber.setText(numbers.get(0));
        } else {
            Object[] array = numbers.toArray();
            CharSequence[] charSeqArray = new CharSequence[array.length];
            for (int i = 0; i < array.length; i++) {
                charSeqArray[i] = (CharSequence) array[i];
            }
            showNumberSelectionDialog(contact.mName, charSeqArray);
        }
    }

    private void initView() {
        findViews();
        bindViewsToNextTarget();
        mMessageEditor.setSendButtonListener(onSendButtonClickListener);
        mMessageEditor.setMenuButtonListener(onMenuButtonClickListener);
    }

    private void showNumberSelectionDialog(final String name,
            final CharSequence[] numbers) {
        final boolean[] checked = new boolean[numbers.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.title_select_number_to_send);

        builder.setItems(numbers, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                mNumber.setText(numbers[item]);
            }
        });

        builder.show();
    }

}
