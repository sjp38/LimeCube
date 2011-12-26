
package org.drykiss.android.app.limecube.data;

import android.content.Context;

import org.drykiss.android.app.limecube.data.ContactsManager.OnContactsDataChangedListener;
import org.drykiss.android.app.limecube.data.TextManager.OnSuggestionChangedListener;

public class DataManager {
    private static DataManager mInstance = null;
    private Context mContext = null;

    private ContactsManager mContactsManager = null;
    private TextManager mTextManager = null;

    private DataManager() {
    }

    public static final DataManager getInstance() {
        if (mInstance == null) {
            mInstance = new DataManager();
        }
        return mInstance;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public void startDataLoading() {
        if (mContactsManager == null) {
            mContactsManager = new ContactsManager(mContext);
        }
        mContactsManager.startLoading();
        
        if (mTextManager == null) {
            mTextManager = new TextManager(mContext);
        }
        mTextManager.loadSuggestions();
    }

    public void stopDataLoading() {
        mContactsManager.stopLoading(false);
    }
    
    public void setOnContactsDataChangedListener(OnContactsDataChangedListener listener) {
        mContactsManager.setOnContactsDataChangedListener(listener);        
    }

    public SimpleContact getContact(int position) {
        return mContactsManager.get(position);
    }
    
    public int getContactsCount() {
        if (mContactsManager == null) {
            return 0;
        }
        return mContactsManager.getContactsCount();
    }
    
    
    public String getSuggestion(int position, SimpleContact contact) {
        return mTextManager.getSuggestion(position, contact);
    }
    
    public int getSuggestionsCount() {
        return mTextManager.getSuggestionsCount();
    }
    
    public boolean updateSuggestion(int position, String suggestion, SimpleContact contact) {
        return mTextManager.updateSuggestion(position, suggestion, contact);
    }
    
    public boolean addSuggestion(String suggestion, SimpleContact contact) {
        return mTextManager.addSuggestion(suggestion, contact);
    }
    
    public boolean removeSuggestion(int position) {
        return mTextManager.removeSuggestion(position);
    }
    
    public void setOnSuggestionChangedListener(OnSuggestionChangedListener listener) {
        mTextManager.setOnSuggestionChangedListener(listener);
    }
}
