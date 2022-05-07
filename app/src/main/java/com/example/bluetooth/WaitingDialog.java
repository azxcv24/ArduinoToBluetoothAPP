package com.example.bluetooth;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.ProgressBar;

public class WaitingDialog extends Dialog {
    public WaitingDialog(Context context){
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wating_layout);
    }
}
