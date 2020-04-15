package com.qrcode.app;

import android.app.Activity;
import android.os.Bundle;

import com.zihuan.app.qrcodelibrary.zxing.view.QrCodeView;

public class MipcaActivityCapture extends Activity {

    QrCodeView qrCodeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        qrCodeView = findViewById(R.id.qrView);
    }


    @Override
    protected void onDestroy() {
        qrCodeView.onDestroy();
        super.onDestroy();
    }
}