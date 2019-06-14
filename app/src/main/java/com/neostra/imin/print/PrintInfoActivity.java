package com.neostra.imin.print;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class PrintInfoActivity extends Activity {
    private TextView mPrintInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_info);
        mPrintInfo = findViewById(R.id.tv_printInfo);

        Intent intent=getIntent();
        String infoValue=intent.getStringExtra("printinfo");

        mPrintInfo.setText(infoValue);
    }
}
