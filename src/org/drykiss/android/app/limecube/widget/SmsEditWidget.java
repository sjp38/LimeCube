
package org.drykiss.android.app.limecube.widget;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.drykiss.android.app.limecube.R;

public class SmsEditWidget extends LinearLayout implements TextWatcher {
    private static final int MAX_MESSAGE_LENGTH = 280;

    private EditText mSmsEditText;
    private ImageButton mMenuButton;
    private ImageButton mSendButton;

    public SmsEditWidget(Context context) {
        super(context);
        initViews(context);
    }

    public SmsEditWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    private void initViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sms_edit_widget_layout, this, true);

        mSmsEditText = (EditText) findViewById(R.id.sms_editText);
        mMenuButton = (ImageButton) findViewById(R.id.sms_edit_menu_button);
        mSendButton = (ImageButton) findViewById(R.id.sms_send_button);
        

        mSmsEditText.addTextChangedListener(this);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(MAX_MESSAGE_LENGTH);
        mSmsEditText.setFilters(filterArray);
        mSendButton.setEnabled(false);
    }
    
    public void setSendButtonListener(View.OnClickListener listener) {
        mSendButton.setOnClickListener(listener);
    }
    
    public void setMenuButtonListener(View.OnClickListener listener) {
        mMenuButton.setOnClickListener(listener);
    }

    public void setText(String text) {
        mSmsEditText.setText(text);
    }
    
    public void addText(String text) {
        final int selectionEnd = mSmsEditText.getSelectionEnd();
        mSmsEditText.getText().replace(selectionEnd,  selectionEnd, text);
        final int newCursorPosition = selectionEnd + text.length();
        mSmsEditText.setSelection(newCursorPosition, newCursorPosition);
    }

    public Editable getText() {
        return mSmsEditText.getText();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final int currentLength = mSmsEditText.getText().toString().length();
        if (currentLength > 0) {
            mSendButton.setEnabled(true);
        } else {
            mSendButton.setEnabled(false);
        }
    }
}
