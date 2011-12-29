
package org.drykiss.android.app.limecube.data;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract.Groups;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupsManager {

    private static final String[] GROUPS_COLUMNS = new String[] {
            Groups.ACCOUNT_NAME,
            Groups.ACCOUNT_TYPE,
            Groups._ID,
            Groups.TITLE,
            Groups.SUMMARY_COUNT
    };

    private static final int ACCOUNT_NAME = 0;
    private static final int ACCOUNT_TYPE = 1;
    private static final int GROUP_ID = 2;
    private static final int TITLE = 3;
    private static final int MEMBER_COUNT = 4;

    private static final String GROUPS_SELECTION = Groups.ACCOUNT_TYPE + " NOT NULL AND "
            + Groups.ACCOUNT_NAME + " NOT NULL AND " + Groups.DELETED + "=0";
    private static final String GROUPS_SORT_ORDER = Groups.ACCOUNT_TYPE + ", "
            + Groups.ACCOUNT_NAME + ", " + Groups.TITLE + " COLLATE LOCALIZED ASC";

    private static final Uri GROUPS_URI = Groups.CONTENT_SUMMARY_URI;

    private static final int QUERY_TOKEN = 0;

    private GroupsQueryHandler mQueryHandler;
    private Context mContext;
    private ArrayList<Group> mGroups = new ArrayList<Group>();
    private OnGroupsDataChangedListener mListener = null;

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            loadGroups();
        }
    };

    public GroupsManager(Context context) {
        mContext = context;
        mQueryHandler = new GroupsQueryHandler(mContext);
    }

    public void startLoading() {
        loadGroups();
        registerContentObserver();
    }

    public void stopLoading(boolean temporal) {
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        if (!temporal) {
            unregisterContentObserver();
        }
    }

    public Group get(int position) {
        return mGroups.get(position);
    }

    public String getTitle(long groupId) {
        for (Group group : mGroups) {
            if (group.mId == groupId) {
                return group.mTitle;
            }
        }
        return null;
    }

    public int getCount() {
        return mGroups.size();
    }

    private void registerContentObserver() {
        mContext.getContentResolver().registerContentObserver(GROUPS_URI, false,
                mContentObserver);
    }

    private void unregisterContentObserver() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private void loadGroups() {
        stopLoading(true);
        mQueryHandler.startQuery(QUERY_TOKEN, null, GROUPS_URI, GROUPS_COLUMNS, GROUPS_SELECTION,
                null, GROUPS_SORT_ORDER);
    }

    public void setOnGroupsDataChangedListener(OnGroupsDataChangedListener listener) {
        mListener = listener;
    }

    private void notifyDataChanged() {
        if (mListener != null) {
            mListener.onGroupsDataChanged();
        }
    }

    private class GroupsQueryHandler extends AsyncQueryHandler {
        public GroupsQueryHandler(Context context) {
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

                final ArrayList<Group> newGroups = new ArrayList<Group>();
                do {
                    final long id = cursor.getLong(GROUP_ID);
                    final String accountType = cursor.getString(ACCOUNT_TYPE);
                    final String accountName = cursor.getString(ACCOUNT_NAME);
                    final String title = cursor.getString(TITLE);
                    final int memberCount = cursor.getInt(MEMBER_COUNT);

                    Group newGroup = new Group(id, memberCount, accountType, accountName, title);
                    newGroups.add(newGroup);
                } while (cursor.moveToNext());
                cursor.close();
                mGroups = newGroups;
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    notifyDataChanged();
                }
            }
        }
    }

    public interface OnGroupsDataChangedListener {
        public void onGroupsDataChanged();
    }
}
