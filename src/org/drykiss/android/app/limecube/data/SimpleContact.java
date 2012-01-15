
package org.drykiss.android.app.limecube.data;

import java.util.ArrayList;

public class SimpleContact {
    public long mId; // read only
    public String mLookupKey; // read only
    public String mName; // read only
    public long mPhotoId;
    public byte[] mPhoto; // read only
    public ArrayList<String> mPhoneNumbers = new ArrayList<String>(); // read
                                                                      // only
    public String mPreferredName; // read/write
    public String mGroup; // read/write

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

    public byte[] getPhoto() {
        return mPhoto;
    }

    public void setPhoto(byte[] photo) {
        mPhoto = photo;
    }

    public void setNumbers(ArrayList<String> phoneNumbers) {
        mPhoneNumbers = phoneNumbers;
    }
}
