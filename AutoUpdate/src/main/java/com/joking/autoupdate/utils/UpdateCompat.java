package com.joking.autoupdate.utils;
/*
 * UpdateCompat     2017-07-06
 * Copyright (c) 2017 JoKing All right reserved.
 */

import android.content.Context;

import java.io.File;

public class UpdateCompat {
    public static void update(Context context, File file) {
        String file_name = file.getAbsolutePath();
        String suffix = file_name.substring(file_name.lastIndexOf(".") + 1).toLowerCase();
        if (suffix.equals("apk")) {
            ApkUtils.installApk(context, file);
        } else if (suffix.equals("patch")) {
            //PatchTask
            String old_apk = ApkUtils.getSourceApkPath(context);
            String new_apk = file_name.substring(0, file_name.lastIndexOf(".")).toLowerCase() + "apk";
            new PatchTask(context).execute(old_apk, new_apk, file_name);
        }
    }
}
