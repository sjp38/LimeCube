
package org.drykiss.android.app.limecube.data;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
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
    private static final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final String[] SIMPLE_CONTACT_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_ID
    };

    private static final String[] SIMPLE_GROUP_MEMBER_PROJECTION = new String[] {
            GroupMembership.CONTACT_ID,
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
    private static final int QUERY_TOKEN_GROUP_MEMBER = 2;

    private static final Uri GROUP_MEMBERS_URI = ContactsContract.Data.CONTENT_URI;
    private static final String GROUP_MEMBERS_SELECTION = GroupMembership.GROUP_ROW_ID + "=? AND "
            + GroupMembership.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'";

    private HashMap<Long, Integer> mContactsMap = new HashMap<Long, Integer>();
    private ArrayList<SimpleContact> mContacts = new ArrayList<SimpleContact>();
    private ArrayList<SimpleContact> mGroupMembers = new ArrayList<SimpleContact>();

    private boolean mGroupMembersLoading = false;

    private ContactsQueryHandler mQueryHandler = null;
    private ContactsPhotoManager mContactsPhotoManager = null;

    private OnContactsDataChangedListener mListener = null;

    private Context mContext;
    private long mGroupId = -1;

    OnPhotoLoadedListener mPhotoLoadedListener = new OnPhotoLoadedListener() {
        @Override
        public void onPhotoLoaded(long contactId, byte[] photo) {
            // Do nothing now. This can be used later for high performance.
            return;
        }

        @Override
        public void onAllRequestedPhotoLoaded() {
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
        if (mGroupId < 0) {
            final int index = getContactsIndex(contactId);
            if (index < 0) {
                return null;
            }
            return mContacts.get(index);
        }
        return null;
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
        mListener = listener;
    }

    public void startLoading(long groupId) {
        mGroupMembersLoading = true;
        mGroupId = groupId;
        loadContacts();
        registerContentObserver();
    }

    public void stopLoading(boolean temporal) {
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        mQueryHandler.cancelOperation(QUERY_TOKEN_GROUP_MEMBER);
        if (!temporal) {
            unregisterContentObserver();
        }
    }

    public SimpleContact get(int position) {
        final SimpleContact contact;
        if (mGroupId >= 0) {
            if (mGroupMembersLoading || position >= mGroupMembers.size()) {
                return null;
            }
            contact = mGroupMembers.get(position);
        } else {
            if (position >= mContacts.size()) {
                return null;
            }
            contact = mContacts.get(position);
        }
        return contact;
    }

    public byte[] getContactPhoto(long contactId, long photoId) {
        return mContactsPhotoManager.get(contactId, photoId);
    }

    public int getContactsCount() {
        if (mGroupId >= 0) {
            if (mGroupMembersLoading) {
                return 0;
            }
            return mGroupMembers.size();
        }
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
        if (mGroupId < 0) {
            mQueryHandler.startQuery(QUERY_TOKEN, null, CONTACTS_URI, SIMPLE_CONTACT_PROJECTION,
                    SIMPLE_CONTACT_SELECTION, null, SIMPLE_CONTACT_SORT_ORDER);
        } else {
            mQueryHandler.startQuery(QUERY_TOKEN_GROUP_MEMBER, null, GROUP_MEMBERS_URI,
                    SIMPLE_GROUP_MEMBER_PROJECTION, GROUP_MEMBERS_SELECTION, new String[] {
                        String.valueOf(mGroupId)
                    }, ContactsContract.Data.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        }
    }

    private class ContactsQueryHandler extends AsyncQueryHandler {
        public ContactsQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            new BindDataTask().execute(cursor);
        }

        private class BindDataTask extends AsyncTask<Cursor, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Cursor... params) {
                final Cursor cursor = params[0];
                if (cursor == null) {
                    return false;
                }
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return false;
                }
                final ArrayList<SimpleContact> newContacts = new ArrayList<SimpleContact>();
                final HashMap<Long, Integer> newContactsMap = new HashMap<Long, Integer>();
                do {
                    final long id = cursor.getLong(SIMPLE_CONTACT_ID_COLUMN_INDEX);
                    final String lookupKey = cursor
                            .getString(SIMPLE_CONTACT_LOOKUP_KEY_COLUMN_INDEX);
                    final String name = cursor.getString(SIMPLE_CONTACT_DISPLAY_NAME_COLUMN_INDEX);
                    final long photoId = cursor.getLong(SIMPLE_CONTACT_PHOTO_ID_COLUMN_INDEX);

                    SimpleContact oldContact = getContact(id);
                    if (oldContact != null) {
                        oldContact.set(lookupKey, name, photoId);
                    } else {
                        oldContact = new SimpleContact(id, lookupKey, name, photoId);
                    }
                    newContacts.add(oldContact);
                    if (mGroupId < 0) {
                        newContactsMap.put(id, newContacts.size() - 1);
                    }
                } while (cursor.moveToNext());
                cursor.close();
                if (mGroupId < 0) {
                    mContacts = newContacts;
                    mContactsMap = newContactsMap;
                } else {
                    mGroupMembers = newContacts;
                    mGroupMembersLoading = false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    notifyListeners();
                }
            }
        }
    }

    private void notifyListeners() {
        if (mListener != null) {
            mListener.onContactsDataChanged();
        }
    }

    public static interface OnContactsDataChangedListener {
        void onContactsDataChanged();
    }
}
