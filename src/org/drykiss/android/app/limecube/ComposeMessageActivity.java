
package org.drykiss.android.app.limecube;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.limecube.ComposeMessageListAdapter.ComposeMessageItem;
import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.DataManager;

import java.util.SortedSet;

public class ComposeMessageActivity extends ActionBarActivity {
    private static final String TAG = "limeCube_compose_message";
    static final int REQUEST_SEND_SMS = 0;
    static final int REQUEST_GET_SUGGESTION = 1;

    private Integer[] mTargetContacts;

    private ListView mListView;
    private ComposeMessageListAdapter mAdapter;
    private CheckBox mSelectAllCheckBox;

    private View mAdView;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_message);

        DataManager.getInstance().startSuggestionsLoading();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        final Object[] received = (Object[]) bundle
                .get(ContactsListActivity.CHECKED_ITEMS_EXTRA_NAME);
        mTargetContacts = new Integer[received.length];
        for (int i = 0; i < received.length; i++) {
            mTargetContacts[i] = (Integer) received[i];
        }

        mSelectAllCheckBox = (CheckBox) findViewById(R.id.messageComposingItemsSelectAllCheckBox);
        mSelectAllCheckBox.setOnCheckedChangeListener(mSelectAllListener);

        mListView = (ListView) findViewById(R.id.messageComposingItemslistView);
        mAdapter = new ComposeMessageListAdapter(this, mTargetContacts);
        mListView.setAdapter(mAdapter);

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
                    mAdapter.onSuggestionSelected();
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

    public static final String COMPOSE_MSG_EXTRA_NAME = "compose_msg_extra_name";
    public static final String COMPOSE_MSG_EXTRA_NUMBER = "compose_msg_extra_number";
    public static final String COMPOSE_MSG_EXTRA_MESSAGE = "compose_msg_extra_message";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_send_message:
                final SortedSet<Integer> checkedItems = mAdapter.getSelectedItems();
                if (checkedItems.size() <= 0) {
                    Toast.makeText(
                            this,
                            R.string.warning_select_contact_first_to_send_messages,
                            Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.warning_bulk_transfer);
                builder.setPositiveButton(R.string.dialog_positive_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Object[] checkedItemsArray = checkedItems.toArray();
                                final int checkedItemsCount = checkedItemsArray.length;
                                String[] names = new String[checkedItemsCount];
                                String[] numbers = new String[checkedItemsCount];
                                String[] messages = new String[checkedItemsCount];
                                for (int i = 0; i < checkedItemsCount; i++) {
                                    ComposeMessageItem composeMessageItem = (ComposeMessageItem) mAdapter
                                            .getItem((Integer) checkedItemsArray[i]);
                                    names[i] = composeMessageItem.mContact.mName;
                                    if (composeMessageItem.mPhoneNumbers.size() <= 0) {
                                        numbers[i] = "";
                                    } else {
                                        numbers[i] = composeMessageItem.mPhoneNumbers
                                                .get(composeMessageItem.mSelectedAddress);
                                    }
                                    messages[i] = composeMessageItem.mMessage;
                                }
                                Intent intent = new Intent(ComposeMessageActivity.this, MessageSenderService.class);

                                intent.putExtra(COMPOSE_MSG_EXTRA_NAME, names);
                                intent.putExtra(COMPOSE_MSG_EXTRA_NUMBER, numbers);
                                intent.putExtra(COMPOSE_MSG_EXTRA_MESSAGE, messages);
                                startService(intent);

                                Intent historyIntent = new Intent(ComposeMessageActivity.this, HistoryActivity.class);
                                startActivity(historyIntent);
                                finish();
                            }
                        });
                builder.setNegativeButton(R.string.dialog_negative_button,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                break;
            default:
                break;
        }
        return true;
    }
}
