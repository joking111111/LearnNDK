package com.joking.app;
/*
 * App     2017-09-21
 * Copyright (c) 2017 JoKing All right reserved.
 */

import android.app.Application;

import com.joking.autoupdate.utils.ApkUtils;
import com.joking.autoupdate.utils.AutoUpdateUtils;

public class App extends Application {

    private static final String API_CHECK_UPDATE = "Your url";

    @Override
    public void onCreate() {
        super.onCreate();

        //初始化更新
        AutoUpdateUtils.Builder builder = new AutoUpdateUtils.Builder()
                //设置更新api
                .setBaseUrl(API_CHECK_UPDATE + ApkUtils.getVersionCode(getApplicationContext()))
                //设置是否显示忽略此版本
                .setIgnoreThisVersion(false)
                //设置下载显示形式 对话框或者通知栏显示 二选一
                .setShowType(AutoUpdateUtils.Builder.TYPE_DIALOG)
                //设置下载时展示的图标
                .setIconRes(R.mipmap.ic_launcher)
                //设置是否打印log日志
                .showLog(true)
                //设置请求方式
                .setRequestMethod(AutoUpdateUtils.Builder.METHOD_GET)
                //设置下载时展示的应用名称
                .setAppName("Your app name")
                .build();
        AutoUpdateUtils.getInstance().init(builder);
    }
}
