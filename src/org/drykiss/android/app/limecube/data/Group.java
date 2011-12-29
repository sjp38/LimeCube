
package org.drykiss.android.app.limecube.data;

public class Group {
    public long mId;
    public int mMemberCount;
    public String mAccountType;
    public String mAccountName;
    public String mTitle;

    public Group(long id, int memberCount, String accountType, String accountName, String title) {
        mId = id;
        mMemberCount = memberCount;
        mAccountType = accountType;
        mAccountName = accountName;
        mTitle = title;
    }

}
