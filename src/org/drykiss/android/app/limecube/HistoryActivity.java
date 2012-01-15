
package org.drykiss.android.app.limecube;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.drykiss.android.app.limecube.ad.AdvertisementManager;
import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.History;
import org.drykiss.android.app.limecube.data.HistoryManager.OnHistoryChangedListener;

public class HistoryActivity extends Activity implements OnHistoryChangedListener {
    private ListView mHistoriesListView;
    private HistoryAdapter mAdapter;

    private View mAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.histories_list);

        DataManager.getInstance().startHistoryLoading();
        DataManager.getInstance().setOnHistoryChangedListener(this);
        mHistoriesListView = (ListView) findViewById(R.id.historiesListView);
        mAdapter = new HistoryAdapter(this);
        mHistoriesListView.setAdapter(mAdapter);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);
    }

    @Override
    public void onHistoryChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private class HistoryAdapter extends BaseAdapter {
        private static final int REMOVE_LOG = 0;
        private static final int RESEND_MSG = 1;
        private Context mContext;

        private View.OnClickListener mMenuButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View parent = (View) v.getParent();
                parent = (View) parent.getParent();
                final HistoryViewHolder viewHolder = (HistoryViewHolder) parent.getTag();
                final CharSequence[] items = mContext.getResources().getTextArray(
                        R.array.history_menu);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case REMOVE_LOG:
                                DataManager.getInstance().removeHistory(viewHolder.mPosition);
                                break;
                            case RESEND_MSG:
                                Intent intent = new Intent(HistoryActivity.this, MessageSenderService.class);

                                final String[] names = {viewHolder.mName.getText().toString()};
                                final String[] numbers = {viewHolder.mAddress.getText().toString()};
                                final String[] messages = {viewHolder.mMessage.getText().toString()};
                                intent.putExtra(ComposeMessageActivity.COMPOSE_MSG_EXTRA_NAME, names);
                                intent.putExtra(ComposeMessageActivity.COMPOSE_MSG_EXTRA_NUMBER, numbers);
                                intent.putExtra(ComposeMessageActivity.COMPOSE_MSG_EXTRA_MESSAGE, messages);
                                startService(intent);
                                break;
                            default:
                                break;

                        }
                    }
                });
                builder.show();
            }
        };

        public HistoryAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public int getCount() {
            return DataManager.getInstance().getHistoriesCount();
        }

        @Override
        public Object getItem(int position) {
            return DataManager.getInstance().getHistory(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HistoryViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_list_item, null, false);

                TextView name = (TextView) convertView.findViewById(R.id.historyNameTextView);
                TextView address = (TextView) convertView.findViewById(R.id.historyAddressTextView);
                ImageButton menuButton = (ImageButton) convertView
                        .findViewById(R.id.historyMenuButton);
                TextView message = (TextView) convertView.findViewById(R.id.historyMessageTextView);
                TextView time = (TextView) convertView.findViewById(R.id.historyTimeTextView);
                TextView result = (TextView) convertView.findViewById(R.id.historyResultTextView);

                menuButton.setOnClickListener(mMenuButtonClickListener);

                viewHolder = new HistoryViewHolder(name, address, menuButton, message, time, result);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (HistoryViewHolder) convertView.getTag();
            }

            History history = DataManager.getInstance().getHistory(position);
            viewHolder.mName.setText(history.mName);
            viewHolder.mAddress.setText(history.mAddress);
            viewHolder.mMessage.setText(history.mMessage);
            viewHolder.mTime.setText(history.mTime);
            viewHolder.mResult.setText(history.mSuccess ? R.string.history_message_sent
                    : R.string.history_failed_to_send_message);

            viewHolder.mPosition = position;
            return convertView;
        }

        private class HistoryViewHolder {
            public HistoryViewHolder(TextView name, TextView address, ImageButton menuButton,
                    TextView message, TextView time, TextView result) {
                mName = name;
                mAddress = address;
                mMenuButton = menuButton;
                mMessage = message;
                mTime = time;
                mResult = result;
            }

            int mPosition;
            TextView mName;
            TextView mAddress;
            ImageButton mMenuButton;
            TextView mMessage;
            TextView mTime;
            TextView mResult;
        }
    }
}
