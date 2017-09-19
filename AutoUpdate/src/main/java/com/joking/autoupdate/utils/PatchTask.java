package com.joking.autoupdate.utils;
/*
 * PatchTask     2017-06-10
 * Copyright (c) 2017 JoKing All right reserved.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.joking.incrementalupdate.BsPatch;

import java.io.File;
import java.lang.ref.WeakReference;


public class PatchTask extends AsyncTask<String, Void, Boolean> {
    private static final String TAG = "PatchTask";

    private String old_apk;
    private String new_apk;
    private String patch;

    private WeakReference<Context> mReference;

    public PatchTask(Context context) {
        mReference = new WeakReference<>(context);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (params.length != 3
                || TextUtils.isEmpty(params[0])
                || TextUtils.isEmpty(params[1])
                || TextUtils.isEmpty(params[2])) {
            return false;
        }

        try {
            old_apk = params[0];
            new_apk = params[1];
            patch = params[2];
            BsPatch.patch(old_apk, new_apk, patch);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mReference.get() != null) {
            if (result) {
                ApkUtils.installApk(mReference.get(), new File(new_apk));
            } else {
                Log.e(TAG, "patch error");
                Toast.makeText(mReference.get(), "patch error!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
