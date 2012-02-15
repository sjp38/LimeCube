
package org.drykiss.android.app.limecube.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;

import java.util.ArrayList;
import java.util.HashMap;

public class ContactsPhotoManager implements Callback {
    private final static int CACHE_MAX_SIZE = 150;
    private final static int CACHE_CLEAR_SIZE = 45;
    private ArrayList<Request> mRequests = new ArrayList<Request>();
    private HashMap<Long, PhotoHolder> mCache = new HashMap<Long, PhotoHolder>();
    private ArrayList<Long> mFlushCandidates = new ArrayList<Long>();
    private HashMap<Long, Long> mNoPhotoCache = new HashMap<Long, Long>();
    private OnPhotoLoadedListener mListener = null;
    private PhotoLoaderThread mLoaderThread = null;
    private final Handler mMainThreadHandler = new Handler(this);
    private static final int MESSAGE_PHOTO_LOADED = 2;
    private static final int MESSAGE_PHOTO_LOADED_ALL = 3;

    public void setOnPhotoLoadedListener(OnPhotoLoadedListener listener) {
        mListener = listener;
    }

    private void notifyPhotoLoaded(PhotoHolder holder) {
        if (mListener != null) {
            mListener.onPhotoLoaded(holder.mContactId, holder.mPhoto);
        }
    }

    private void notifyPhotoLoadedAll() {
        if (mListener != null) {
            mListener.onAllRequestedPhotoLoaded();
        }
    }

    public void clearNoPhotoCache() {
        mNoPhotoCache.clear();
    }

    public byte[] get(long contactId, long photoId) {
        if (mNoPhotoCache.get(contactId) != null) {
            return null;
        }
        PhotoHolder holder = mCache.get(contactId);
        if (holder != null) {
            return holder.mPhoto;
        }
        final Request request = new Request(contactId, photoId);
        if (mRequests.contains(request)) {
            return null;
        }

        mRequests.add(request);
        if (mLoaderThread == null) {
            mLoaderThread = new PhotoLoaderThread();
            mLoaderThread.start();
        }
        mLoaderThread.requestLoading();
        return null;
    }

    private class Request {
        public long mContactId;
        public long mPhotoId;

        public Request(long contactId, long photoId) {
            mContactId = contactId;
            mPhotoId = photoId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (int) (mContactId ^ (mContactId >>> 32));
            result = prime * result + (int) (mPhotoId ^ (mPhotoId >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Request other = (Request) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (mContactId != other.mContactId)
                return false;
            if (mPhotoId != other.mPhotoId)
                return false;
            return true;
        }

        private ContactsPhotoManager getOuterType() {
            return ContactsPhotoManager.this;
        }
    }

    private class PhotoHolder {
        public byte[] mPhoto;
        public long mContactId;

        public PhotoHolder(byte[] photo, long contactId) {
            mPhoto = photo;
            mContactId = contactId;
        }
    }

    private class PhotoLoaderThread extends HandlerThread implements Callback {
        private static final int MESSAGE_LOAD_PHOTO = 1;
        // When all request complete, wait this mili-seconds before notice.
        private static final int ALL_REQUEST_LOADED_NOTICE_DELAY = 300;
        private boolean mLoading = false;
        private Handler mLoaderThreadHandler;
        private static final String PHOTO_LOADER_THREAD_NAME = "ContactsPhotoLoader";

        public PhotoLoaderThread() {
            super(PHOTO_LOADER_THREAD_NAME);
        }

        public void requestLoading() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }

            mLoaderThreadHandler.sendEmptyMessage(MESSAGE_LOAD_PHOTO);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOAD_PHOTO:
                    if (!mLoading) {
                        processRequests();
                    }
            }
            return true;
        }

        synchronized public void processRequests() {
            mLoading = true;
            final ContentResolver resolver = DataManager.getInstance().getContext()
                    .getContentResolver();
            while (mRequests.size() != 0) {
                Request request = mRequests.get(0);
                final long contactId = request.mContactId;
                final long photoId = request.mPhotoId;
                mRequests.remove(request);
                final Cursor cursor = resolver.query(Data.CONTENT_URI, new String[] {
                        Photo.PHOTO
                }, Photo._ID + " IN(" + photoId + ")", null, null);
                if (cursor == null) {
                    continue;
                }
                if (!cursor.moveToFirst()) {
                    mNoPhotoCache.put(request.mContactId, request.mPhotoId);
                    cursor.close();
                    continue;
                }
                byte[] photo = cursor.getBlob(0);
                cursor.close();
                if (photo == null) {
                    continue;
                }
                final PhotoHolder photoHolder = new PhotoHolder(photo, contactId);
                mCache.put(contactId, photoHolder);
                if (!mFlushCandidates.contains(contactId)) {
                    mFlushCandidates.add(contactId);
                }
                if (mCache.size() > CACHE_MAX_SIZE) {
                    for (int i = 0; i < CACHE_CLEAR_SIZE; i++) {
                        mCache.remove(mFlushCandidates.get(0));
                        mFlushCandidates.remove(0);
                    }
                }
                final Message msg = new Message();
                msg.what = MESSAGE_PHOTO_LOADED;
                msg.obj = photoHolder;
                mMainThreadHandler.sendMessage(msg);
            }
            mLoading = false;
            mMainThreadHandler.removeMessages(MESSAGE_PHOTO_LOADED_ALL);
            mMainThreadHandler.sendEmptyMessageDelayed(MESSAGE_PHOTO_LOADED_ALL,
                    ALL_REQUEST_LOADED_NOTICE_DELAY);
        }
    }

    public interface OnPhotoLoadedListener {
        /**
         * Called every time photo loaded.
         * 
         * @param contactId Loaded photo's contact id.
         * @param photo Loaded photo in bitmap.
         */
        public void onPhotoLoaded(long contactId, byte[] photo);

        /**
         * Called when every requested photo loaded.
         */
        public void onAllRequestedPhotoLoaded();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_PHOTO_LOADED:
                notifyPhotoLoaded((PhotoHolder) msg.obj);
                break;
            case MESSAGE_PHOTO_LOADED_ALL:
                notifyPhotoLoadedAll();
            default:
                break;
        }
        return false;
    }
}
