
package org.drykiss.android.app.limecube.data;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import org.drykiss.android.app.limecube.data.ContactsPhotoManager.OnPhotoLoadedListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Use Android API, load contacts data from contacts provider and store as
 * SimpleContact's set.
 * 
 * @author sj38_park
 */
public class ContactsManager {
    private static final String TAG = "ContactsManager";
    private static final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final String[] SIMPLE_CONTACT_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_ID
    };

    private static final int SIMPLE_CONTACT_ID_COLUMN_INDEX = 0;
    private static final int SIMPLE_CONTACT_LOOKUP_KEY_COLUMN_INDEX = 1;
    private static final int SIMPLE_CONTACT_DISPLAY_NAME_COLUMN_INDEX = 2;
    private static final int SIMPLE_CONTACT_PHOTO_ID_COLUMN_INDEX = 3;

    private static final String SIMPLE_CONTACT_SELECTION = Contacts.HAS_PHONE_NUMBER + "=1";
    private static final String SIMPLE_CONTACT_SORT_ORDER = ContactsContract.Contacts.DISPLAY_NAME
            + " COLLATE LOCALIZED ASC";
    private static final int QUERY_TOKEN = 1;

    private HashMap<Long, Integer> mContactsMap = new HashMap<Long, Integer>();
    private ArrayList<SimpleContact> mContacts = new ArrayList<SimpleContact>();

    private ContactsQueryHandler mQueryHandler = null;
    private ContactsPhotoManager mContactsPhotoManager = null;

    private ArrayList<OnContactsDataChangedListener> mListeners = new ArrayList<OnContactsDataChangedListener>();

    private Context mContext;

    OnPhotoLoadedListener mPhotoLoadedListener = new OnPhotoLoadedListener() {
        @Override
        public void onPhotoLoaded(long contactId, byte[] photo) {
            final SimpleContact contact = getContact(contactId);
            if (contact == null) {
                return;
            }
            contact.setPhoto(photo);
            notifyListeners();
        }
    };

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mContactsPhotoManager.clearNoPhotoCache();
            loadContacts();
        }
    };

    private SimpleContact getContact(long contactId) {
        final int index = getContactsIndex(contactId);
        if (index < 0) {
            return null;
        }
        return mContacts.get(index);
    }

    private int getContactsIndex(long contactId) {
        final Integer index = mContactsMap.get(contactId);
        if (index == null) {
            return -1;
        }
        return index;
    }

    public ContactsManager(Context context) {
        mContext = context;
        mQueryHandler = new ContactsQueryHandler(mContext);
        mContactsPhotoManager = new ContactsPhotoManager();
        mContactsPhotoManager.setOnPhotoLoadedListener(mPhotoLoadedListener);
    }

    public void setOnContactsDataChangedListener(OnContactsDataChangedListener listener) {
        mListeners.add(listener);
    }

    public void startLoading() {
        loadContacts();
        registerContentObserver();
    }

    public void stopLoading(boolean temporal) {
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        if (!temporal) {
            unregisterContentObserver();
        }
    }

    public SimpleContact get(int position) {
        if (position >= mContacts.size()) {
            return null;
        }
        final SimpleContact contact = mContacts.get(position);
        contact.setPhoto(mContactsPhotoManager.get(contact.mId, contact.mPhotoId));
        return contact;
    }

    public int getContactsCount() {
        return mContacts.size();
    }

    private void registerContentObserver() {
        mContext.getContentResolver().registerContentObserver(CONTACTS_URI, false,
                mContentObserver);
    }

    private void unregisterContentObserver() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private void loadContacts() {
        stopLoading(true);
        mQueryHandler.startQuery(QUERY_TOKEN, null, CONTACTS_URI, SIMPLE_CONTACT_PROJECTION,
                SIMPLE_CONTACT_SELECTION, null, SIMPLE_CONTACT_SORT_ORDER);
    }

    private class ContactsQueryHandler extends AsyncQueryHandler {
        public ContactsQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor == null) {
                return;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return;
            }

            final ArrayList<SimpleContact> newContacts = new ArrayList<SimpleContact>();
            final HashMap<Long, Integer> newContactsMap = new HashMap<Long, Integer>();
            do {
                final long id = cursor.getLong(SIMPLE_CONTACT_ID_COLUMN_INDEX);
                final String lookupKey = cursor.getString(SIMPLE_CONTACT_LOOKUP_KEY_COLUMN_INDEX);
                final String name = cursor.getString(SIMPLE_CONTACT_DISPLAY_NAME_COLUMN_INDEX);
                final long photoId = cursor.getLong(SIMPLE_CONTACT_PHOTO_ID_COLUMN_INDEX);

                SimpleContact oldContact = getContact(id);
                if (oldContact != null) {
                    oldContact.set(lookupKey, name, photoId);
                } else {
                    oldContact = new SimpleContact(id, lookupKey, name, photoId);
                }
                newContacts.add(oldContact);
                newContactsMap.put(id, newContacts.size() - 1);
            } while (cursor.moveToNext());
            cursor.close();
            mContacts = newContacts;
            mContactsMap = newContactsMap;
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (OnContactsDataChangedListener listener : mListeners) {
            listener.onContactsDataChanged();
        }
    }

    public static interface OnContactsDataChangedListener {
        void onContactsDataChanged();
    }
}
