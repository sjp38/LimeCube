
package org.drykiss.android.app.limecube.data;


public class SimpleContact {
    public long mId;
    public String mLookupKey;
    public String mName;
    public long mPhotoId;

    public SimpleContact(long id, String lookupKey, String name, long photoId) {
        mId = id;
        mLookupKey = lookupKey;
        mName = name;
        mPhotoId = photoId;
    }

    public void set(String lookupKey, String name, long photoId) {
        mLookupKey = lookupKey;
        mName = name;
        mPhotoId = photoId;
    }
}
