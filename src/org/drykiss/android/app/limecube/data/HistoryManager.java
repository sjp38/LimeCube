
package org.drykiss.android.app.limecube.data;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class HistoryManager {
    private static final String TAG = "LimeCube_HistoryManager";
    private static final String DATA_FILE_PATH = "/data/data/org.drykiss.android.app.limecube/limeCube_history.lcx";
    private static final int MAX_HISTORY = 900;

    private boolean mDataLoaded = false;

    private ArrayList<History> mHistories = new ArrayList<History>();
    private OnHistoryChangedListener mListener;

    public HistoryManager() {
    }

    public synchronized void loadHistories() {
        if (mDataLoaded) {
            return;
        }
        File file = new File(DATA_FILE_PATH);
        try {
            Log.i(TAG, "load history data.");
            FileInputStream fis = new FileInputStream(file);
            ObjectInput serialized = new ObjectInputStream(fis);
            ArrayList<History> histories = (ArrayList<History>) serialized.readObject();
            serialized.close();
            fis.close();
            mHistories = histories;

            mDataLoaded = true;
        } catch (IOException e) {
            Log.e(TAG, "IOException while read data file!", e);
            return;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found.", e);
        }
        return;
    }

    public synchronized boolean saveHistories() {
        Log.i(TAG, "save history.");
        final File file = new File(DATA_FILE_PATH);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final FileOutputStream fos = new FileOutputStream(file);
            ObjectOutput serialized = new ObjectOutputStream(fos);
            serialized.writeObject(mHistories);
            serialized.flush();
            serialized.close();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException while save data file!", e);
            return false;
        }
        notifyListener();
        return true;
    }

    public History getHistory(int position) {
        if (position >= mHistories.size()) {
            return null;
        }
        return mHistories.get(position);
    }

    public boolean addHistory(History history) {
        mHistories.add(0, history);
        if (mHistories.size() >= MAX_HISTORY) {
            mHistories.remove(mHistories.size() - 1);
        }
        saveHistories();
        return true;
    }

    public boolean removeHistory(int position) {
        if (position >= mHistories.size()) {
            return false;
        }
        mHistories.remove(position);
        saveHistories();
        return true;
    }

    public int getHistoriesCount() {
        return mHistories.size();
    }

    private void notifyListener() {
        if (mListener != null) {
            mListener.onHistoryChanged();
        }
    }

    public void setOnHistoryChangedListener(OnHistoryChangedListener listener) {
        mListener = listener;
    }

    public interface OnHistoryChangedListener {
        public void onHistoryChanged();
    }

}
