package com.zihuan.app.qrcodelibrary.zxing;

import android.content.Intent;

import com.google.zxing.Result;

import org.jetbrains.annotations.NotNull;

public interface QrCodeScanResultListener {
    void qrSuccessful(int result, Intent intent);


    void qrFailure(@NotNull Result result);
}
