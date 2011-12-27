
package org.drykiss.android.app.limecube.data;

import android.content.Context;
import android.util.Log;

import org.drykiss.android.app.limecube.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Remember user's input, gives best suggestion.
 * 
 * @author sj38_park
 */
public class TextManager {
    private static final String TAG = "LimeCube_TextManager";
    private static final String DATA_FILE_PATH = "/data/data/org.drykiss.android.app.limecube/limeCube_data.lcx";
    private static final int MAX_SUGGESTIONS = 90;

    private boolean mDataLoaded = false;

    private Context mContext;

    private ArrayList<String> mSuggestions = new ArrayList<String>();
    private OnSuggestionChangedListener mListener;

    public TextManager(Context context) {
        mContext = context;
    }

    /**
     * Get block settings. If this is first time, read from file.
     * 
     * @return
     */
    public void loadSuggestions() {
        if (mSuggestions.size() == 0) {
            String[] defaultSuggestions = mContext.getResources().getStringArray(
                    R.array.default_suggestions);
            for (String suggestion : defaultSuggestions) {
                mSuggestions.add(0, suggestion);
            }
        }
        if (mDataLoaded) {
            return;
        }
        File file = new File(DATA_FILE_PATH);
        try {
            Log.i(TAG, "load setting data.");
            FileInputStream fis = new FileInputStream(file);
            ObjectInput serialized = new ObjectInputStream(fis);
            ArrayList<String> suggestions = (ArrayList<String>) serialized.readObject();
            serialized.close();
            fis.close();
            mSuggestions = suggestions;

            mDataLoaded = true;
        } catch (IOException e) {
            Log.e(TAG, "IOException while read data file!", e);
            return;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found.", e);
        }
        return;
    }

    public synchronized boolean saveSuggestion() {
        Log.i(TAG, "save setting data.");
        final File file = new File(DATA_FILE_PATH);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final FileOutputStream fos = new FileOutputStream(file);
            ObjectOutput serialized = new ObjectOutputStream(fos);
            serialized.writeObject(mSuggestions);
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

    public String getSuggestion(int position, SimpleContact contact) {
        if (position >= mSuggestions.size()) {
            return null;
        }
        String suggestion = mSuggestions.get(position);
        return LimeCoder.decode(suggestion, contact.mName);
    }

    public boolean updateSuggestion(int position, String suggestion, SimpleContact contact) {
        suggestion = LimeCoder.encode(suggestion, contact.mName);
        if (mSuggestions.contains(suggestion)) {
            return false;
        }
        mSuggestions.remove(position);
        mSuggestions.add(position, suggestion);
        saveSuggestion();
        return true;
    }

    public boolean addSuggestion(String suggestion, SimpleContact contact) {
        suggestion = LimeCoder.encode(suggestion, contact.mName);
        if (mSuggestions.contains(suggestion)) {
            return false;
        }
        mSuggestions.add(0, suggestion);
        if (mSuggestions.size() > MAX_SUGGESTIONS) {
            mSuggestions.remove(0);
        }
        saveSuggestion();
        return true;
    }

    public boolean removeSuggestion(int position) {
        if (position >= mSuggestions.size()) {
            return false;
        }
        mSuggestions.remove(position);
        saveSuggestion();
        return true;
    }

    public int getSuggestionsCount() {
        return mSuggestions.size();
    }

    private void notifyListener() {
        if (mListener != null) {
            mListener.onSuggestionChanged();
        }
    }

    public void setOnSuggestionChangedListener(OnSuggestionChangedListener listener) {
        mListener = listener;
    }

    public interface OnSuggestionChangedListener {
        public void onSuggestionChanged();
    }

}
