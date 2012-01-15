
package org.drykiss.android.app.limecube;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.SimpleContact;
import org.drykiss.android.app.limecube.data.TextManager.OnSuggestionChangedListener;

public class SuggestionsActivity extends ActionBarActivity {
    private static final String TAG = "LimeCube_suggestions";
    public static final String CONTACT_POSITION_EXTRA = "contact_position_extra";
    public static final String SUGGESTION_EXTRA = "suggestion_extra";

    private static final int EDIT_MENU = 0;
    private static final int REMOVE_MENU = 1;

    private SimpleContact mContact;

    private ListView mListView;
    private SuggestionListAdapter mAdapter;

    // for advertise.
    private View mAdView;

    private OnSuggestionChangedListener mSuggestionChangedListener = new OnSuggestionChangedListener() {
        @Override
        public void onSuggestionChanged() {
            Log.i(TAG, "suggestion data changed.");
            mAdapter.notifyDataSetChanged();
        }
    };

    private View.OnClickListener mSuggestionClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SuggestionListItemViewHolder holder = (SuggestionListItemViewHolder) v.getTag();
            String selected = holder.mSuggestion.getText().toString();
            DataManager.getInstance().addSuggestion(selected, mContact);

            Intent intent = new Intent();
            intent.putExtra(SUGGESTION_EXTRA, selected);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private View.OnClickListener mMenuButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View parent = (View) v.getParent();
            SuggestionListItemViewHolder holder = (SuggestionListItemViewHolder) parent.getTag();
            final int position = holder.mPosition;
            final String currentSuggestion = holder.mSuggestion.getText().toString();

            final CharSequence[] items;
            items = getResources().getTextArray(
                    R.array.suggestion_edit_menu);

            AlertDialog.Builder builder = new AlertDialog.Builder(SuggestionsActivity.this);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case EDIT_MENU:
                            showSuggestionEditDialog(currentSuggestion, position);

                            break;
                        case REMOVE_MENU:
                            DataManager.getInstance().removeSuggestion(position);
                            break;
                        default:
                            break;
                    }
                }
            });
            builder.show();
        }
    };

    private void showSuggestionEditDialog(final String currentSuggestion, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setText(currentSuggestion);
        if (position == -1) {
            builder.setTitle(R.string.suggestion_add_dialog_title);
        } else {
            builder.setTitle(R.string.suggestion_edit_dialog_title);
        }
        builder.setView(input);

        builder.setPositiveButton(R.string.dialog_positive_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String text = input.getText().toString();
                        if (text.length() == 0) {
                            Toast.makeText(SuggestionsActivity.this,
                                    R.string.warning_suggestion_should_not_empty,
                                    Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                            return;
                        }
                        if (position == -1) {
                            if (!DataManager.getInstance().addSuggestion(text, mContact)) {
                                Toast.makeText(SuggestionsActivity.this,
                                        R.string.failed_to_add_suggestion, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                        else if (!DataManager.getInstance().updateSuggestion(position, text,
                                mContact)) {
                            Toast.makeText(SuggestionsActivity.this,
                                    R.string.failed_to_update_suggestion, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
        builder.setNegativeButton(R.string.dialog_negative_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suggestions);

        Intent intent = getIntent();
        int position = intent.getIntExtra(CONTACT_POSITION_EXTRA, -1);
        mContact = DataManager.getInstance().getContact(position);

        mListView = (ListView) findViewById(R.id.suggestionslistView);
        mAdapter = new SuggestionListAdapter(this);
        mListView.setAdapter(mAdapter);

        DataManager.getInstance().setOnSuggestionChangedListener(mSuggestionChangedListener);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    public void onDestroy() {
        AdvertisementManager.destroyAd(mAdView);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.suggestions_activity_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_add_suggestion:
                showSuggestionEditDialog("", -1);
                break;
            default:
                break;
        }
        return true;
    }

    private class SuggestionListAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater = null;

        public SuggestionListAdapter(Context context) {
            super();
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return DataManager.getInstance().getSuggestionsCount();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View suggestionListItem;
            SuggestionListItemViewHolder holder;
            if (convertView == null) {
                suggestionListItem = mLayoutInflater.inflate(R.layout.suggestion_list_item,
                        null, false);
                holder = new SuggestionListItemViewHolder();

                holder.mSuggestion = (TextView) suggestionListItem
                        .findViewById(R.id.suggestion_textView);
                holder.mMenuButton = (ImageButton) suggestionListItem
                        .findViewById(R.id.suggestion_edit_button);

                suggestionListItem.setOnClickListener(mSuggestionClickedListener);
                holder.mMenuButton.setOnClickListener(mMenuButtonClickedListener);
                suggestionListItem.setTag(holder);
            } else {
                suggestionListItem = convertView;
                holder = (SuggestionListItemViewHolder) suggestionListItem.getTag();
            }

            String suggestion = DataManager.getInstance().getSuggestion(position, mContact);
            holder.mSuggestion.setText(suggestion);
            holder.mPosition = position;

            return suggestionListItem;
        }

        @Override
        public Object getItem(int position) {
            return DataManager.getInstance().getSuggestion(position, mContact);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private class SuggestionListItemViewHolder {
        int mPosition;
        TextView mSuggestion;
        ImageButton mMenuButton;
    }
}
