
package org.drykiss.android.app.limecube.data;

import java.io.Serializable;

public class History implements Serializable {
    private static final long serialVersionUID = -4768107426196331436L;

    public String mName;
    public String mAddress;
    public String mMessage;
    public String mTime;
    public boolean mSuccess;

    public History(String name, String address, String message, String time, boolean success) {
        mName = name;
        mAddress = address;
        mMessage = message;
        mTime = time;
        mSuccess = success;
    }

    public void setSuccess(boolean success) {
        mSuccess = success;
    }
}
