package com.joking.autoupdate.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.joking.autoupdate.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadService extends Service {
    private static final int NOTIFY_ID = 0;

    private Context mContext = this;
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private Notification.Builder builder;
    private DownFileAsyncTask downFileAsyncTask;
    private String downUrl;
    private String appName;
    private int showType;
    private int lastPosition;
    private boolean autoCancel;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            //此时service由于内存紧张而被系统杀死并重新创建的，Intent为空
            Toast.makeText(mContext, "请重新检查更新", Toast.LENGTH_SHORT).show();
        } else {
            downUrl = intent.getStringExtra("downUrl");
            appName = intent.getStringExtra("appName");
            showType = intent.getIntExtra("type", 0);
            if (!TextUtils.isEmpty(downUrl)) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    if (showType == AutoUpdateUtils.Builder.TYPE_DIALOG) {
                        mNotificationManager = null;
                    } else {
                        builder = new Notification.Builder(mContext);
                        if (intent.getIntExtra("icRes", 0) != 0) {
                            builder.setSmallIcon(intent.getIntExtra("icRes", 0));
                        } else {
                            builder.setSmallIcon(R.mipmap.ic_launcher); //设置图标
                        }
                        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.layout_notification);
                        if (TextUtils.isEmpty(appName)) {
                            contentView.setTextViewText(R.id.fileName, "正在下载...");
                        } else {
                            contentView.setTextViewText(R.id.fileName, appName + "正在下载...");
                        }
                        builder.setContent(contentView);
                        mNotification = builder.build();
                        mNotificationManager.notify(NOTIFY_ID, mNotification);
                    }
                    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
//                final String fileName = filePath + "/" + getPackgeName(this) + "-v" + getVersionName(this) + ".apk";

                    String suffix = downUrl.substring(downUrl.lastIndexOf(".")).toLowerCase();
                    final String fileName = filePath + "/" + mContext.getPackageName() + "-v" + ApkUtils.getVersionName(mContext) + suffix;

                    downFileAsyncTask = new DownFileAsyncTask();
                    downFileAsyncTask.execute(fileName);
                } else {
                    Toast.makeText(mContext, "外部存储空间不足", Toast.LENGTH_SHORT).show();
                }
            } else {
                throw new RuntimeException("下载地址不能为空");
            }
        }
//        return super.onStartCommand(intent, flags, startId);
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downFileAsyncTask != null) {
            downFileAsyncTask.cancel(true);
        }
    }

    //异步任务下载文件
    private class DownFileAsyncTask extends AsyncTask<String, Integer, File> {

        @Override
        protected File doInBackground(String... params) {
            try {
                FileOutputStream fos;
                BufferedInputStream bis;
                HttpURLConnection httpUrl;
                URL url;
                byte[] buf = new byte[2048];
                int size;

                //建立链接
                url = new URL(downUrl);
                httpUrl = (HttpURLConnection) url.openConnection();
                //设置网络连接超时时间5S
                httpUrl.setConnectTimeout(10 * 1000);
                //连接指定的资源
                httpUrl.connect();
                //获取网络输入流
                bis = new BufferedInputStream(httpUrl.getInputStream());
                //建立文件
                File file = new File(params[0]);
                if (!file.exists()) {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    file.createNewFile();
                }
                fos = new FileOutputStream(params[0]);

                final long total = httpUrl.getContentLength();
                long sum = 0;

                //保存文件
                while ((size = bis.read(buf)) != -1) {
                    sum += size;
                    fos.write(buf, 0, size);
                    publishProgress((int) (sum * 100 / total));
                }
                fos.close();
                bis.close();
                httpUrl.disconnect();
                return file;
            } catch (IOException e) {
                //发送特定action的广播
                Intent intent = new Intent();
                intent.setAction("android.intent.action.MY_RECEIVER");
                intent.putExtra("type", "err");
                intent.putExtra("err", e.toString());
//                sendBroadcast(intent);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (!autoCancel)
                Toast.makeText(mContext, "已取消下载", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file != null) {
//                installApkFile(mContext, file);
                UpdateCompat.update(mContext, file);
            } else {
                if (showType != AutoUpdateUtils.Builder.TYPE_DIALOG) {
                    mNotificationManager.cancel(NOTIFY_ID);
                    mNotification.contentView = null;
                }
            }
            stopSelf(); //停止服务
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int rate = values[0];
            //   if ((rate % 6 == 0 || rate == 100) && rate != lastPosition) {
            if ((rate < 100 || rate == 100) && rate != lastPosition) {
                lastPosition = rate;
                if (showType != AutoUpdateUtils.Builder.TYPE_DIALOG) {
                    if (rate < 100) {
                        //更新进度
                        RemoteViews contentView = mNotification.contentView;
                        contentView.setTextViewText(R.id.rate, rate + "%");
                        contentView.setProgressBar(R.id.progress, 100, rate, false);
                        // 最后别忘了通知一下,否则不会更新
                        mNotificationManager.notify(NOTIFY_ID, mNotification);
                    } else {
                        //下载完毕后变换通知形式
                        mNotificationManager.cancel(NOTIFY_ID);
                        mNotification.contentView = null;
                    }

                    autoCancel = false;
                    if (rate >= 100) {
                        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                        autoCancel = true;
                    }
                }
                //发送特定action的广播
                Intent intent = new Intent();
                intent.setAction("android.intent.action.MY_RECEIVER");
                intent.putExtra("type", "doing");
                intent.putExtra("progress", rate);
//                sendBroadcast(intent);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}  