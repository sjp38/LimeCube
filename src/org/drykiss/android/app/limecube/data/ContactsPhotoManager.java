
package org.drykiss.android.app.limecube.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * We should use cache. But, first, start with brute force.
 * 
 * @author sj38_park
 */
public class ContactsPhotoManager implements Callback {
    private final static int CACHE_MAX_SIZE = 100;
    private final static int CACHE_CLEAR_SIZE = 30;
    private ArrayList<Request> mRequests = new ArrayList<Request>();
    private HashMap<Long, PhotoHolder> mCache = new HashMap<Long, PhotoHolder>();
    private ArrayList<Long> mFlushCandidates = new ArrayList<Long>();
    private HashMap<Long, Long> mNoPhotoCache = new HashMap<Long, Long>();
    private OnPhotoLoadedListener mListener = null;
    private PhotoLoaderThread mLoaderThread = null;
    private final Handler mMainThreadHandler = new Handler(this);
    private static final int MESSAGE_PHOTO_LOADED = 2;

    public void setOnPhotoLoadedListener(OnPhotoLoadedListener listener) {
        mListener = listener;
    }

    private void notifyPhotoLoaded(PhotoHolder holder) {
        if (mListener != null) {
            final byte[] photo = holder.mPhoto.get();
            if (photo != null) {
                mListener.onPhotoLoaded(holder.mContactId, photo);
            }
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
            final byte[] photo = holder.mPhoto.get();
            if (photo != null) {
                return photo;
            } else {
                mCache.remove(contactId);
            }
        }
        mRequests.add(new Request(contactId, photoId));
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
    }

    private class PhotoHolder {
        public WeakReference<byte[]> mPhoto;
        public long mContactId;

        public PhotoHolder(byte[] photo, long contactId) {
            mPhoto = new WeakReference<byte[]>(photo);
            mContactId = contactId;
        }
    }

    private class PhotoLoaderThread extends HandlerThread implements Callback {
        private static final int MESSAGE_LOAD_PHOTO = 1;
        private boolean mLoading = false;
        private Handler mLoaderThreadHandler;
        private static final String PHOTO_LOADER_THREAD_NAME = "ContactsPhotoLoader";

        public PhotoLoaderThread() {
            super(PHOTO_LOADER_THREAD_NAME);
            // TODO Auto-generated constructor stub
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
        }
    }

    public interface OnPhotoLoadedListener {
        public void onPhotoLoaded(long contactId, byte[] photo);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_PHOTO_LOADED:
                notifyPhotoLoaded((PhotoHolder) msg.obj);
        }
        // TODO Auto-generated method stub
        return false;
    }
}
