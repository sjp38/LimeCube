
package org.drykiss.android.app.limecube.data;

import java.util.ArrayList;

public class Group {
    public String mName;
    public ArrayList<Long> mMembers = new ArrayList<Long>();
    public ArrayList<String> mMessages = new ArrayList<String>();

    public Group(String name) {
        mName = name;
    }

    public Group(String name, ArrayList<Long> members, ArrayList<String> messages) {
        mName = name;
        mMembers = members;
        mMessages = messages;
    }

}
