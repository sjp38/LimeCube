
package org.drykiss.android.app.limecube;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.drykiss.android.app.limecube.data.DataManager;
import org.drykiss.android.app.limecube.data.History;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageSenderService extends Service {
    private static final String TAG = "LimeCube_MessageSender";
    private static final String ACTION_SMS_SENT = "org.drykiss.android.app.limecube.sms_sent";

    private Object[] mTargetNames = null;
    private Object[] mTargetNumbers = null;
    private Object[] mTargetMessages = null;
    private int mCurrentSending = 0;
    private boolean mSending = false;
    private ArrayList<Intent> mWaiting = new ArrayList<Intent>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int sent = mCurrentSending;
                final long time = System.currentTimeMillis();
                final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss");
                final Timestamp timeStamp = new Timestamp(time);
                String sentTime = format.format(timeStamp);

                History history = new History((String) mTargetNames[sent],
                        (String) mTargetNumbers[sent], (String) mTargetMessages[sent], sentTime,
                        true);
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        break;
                    default:
                        history.setSuccess(false);
                        break;
                }
                DataManager.getInstance().addHistory(history);
                sendNextMessage();
            }
        }, new IntentFilter(ACTION_SMS_SENT));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWaiting.add(intent);
        if (!mSending) {
            doNextWork();
        }
        return START_STICKY_COMPATIBILITY;
    }

    private void sendNextMessage() {
        if (++mCurrentSending < mTargetNames.length) {
            sendSms();
        } else {
            mCurrentSending = 0;
            mSending = false;
            mTargetNames = null;
            mTargetNumbers = null;
            mTargetMessages = null;
            mWaiting.remove(0);
            doNextWork();
        }
    }

    private void doNextWork() {
        if (mWaiting.size() <= 0) {
            return;
        }
        mSending = true;
        Intent intent = mWaiting.get(0);
        Bundle bundle = intent.getExtras();
        mTargetNames = (Object[]) bundle.get(ComposeMessageActivity.COMPOSE_MSG_EXTRA_NAME);
        mTargetNumbers = (Object[]) bundle.get(ComposeMessageActivity.COMPOSE_MSG_EXTRA_NUMBER);
        mTargetMessages = (Object[]) bundle.get(ComposeMessageActivity.COMPOSE_MSG_EXTRA_MESSAGE);
        sendSms();
    }

    private void sendSms() {
        if (TextUtils.isEmpty((String) mTargetNumbers[mCurrentSending])) {
            sendNextMessage();
            return;
        }
        final SmsManager manager = SmsManager.getDefault();
        ArrayList<String> messages = manager
                .divideMessage((String) mTargetMessages[mCurrentSending]);
        for (String splittedMsg : messages) {
            try {
                manager.sendTextMessage((String) mTargetNumbers[mCurrentSending], null,
                        splittedMsg,
                        PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), 0), null);
            } catch (SecurityException e) {
                Log.e(TAG, "Maybe manufacturer modified security in wrong way!", e);
            } catch (NullPointerException e) {
                Log.d(TAG, "Maybe framework's fault. try again.", e);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interruptException) {
                    Log.d(TAG, "Failed to sleep for one more try. Do it now.");
                }
                try {
                    manager.sendTextMessage((String) mTargetNumbers[mCurrentSending], null,
                            splittedMsg,
                            PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), 0),
                            null);
                } catch (NullPointerException secondException) {
                    Log.e(TAG, "Maybe framework's fault. show erro message to user.",
                            secondException);
                    final String msg = getString(R.string.error_can_not_send_message,
                            mTargetNumbers[mCurrentSending], splittedMsg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
