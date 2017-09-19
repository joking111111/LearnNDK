package com.joking.autoupdate.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.joking.autoupdate.R;
import com.joking.autoupdate.model.LibraryUpdateEntity;
import com.joking.autoupdate.model.UpdateEntity;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION_CODES.M;
import static com.joking.autoupdate.utils.ApkUtils.getVersionCode;

public class AutoUpdateUtils {

    private static int PERMISSON_REQUEST_CODE = 2;

    //applicaationcontext
    private Context appContext;
    //非applicaationcontext
    private Context mContext;
    //广播接受者
    private MyReceiver receiver;
    //定义一个展示下载进度的进度条
    private ProgressDialog progressDialog;
    //检查更新的url
    private String checkUrl;
    //展示下载进度的方式 对话框模式 通知栏进度条模式
    private int showType = Builder.TYPE_DIALOG;
    //是否展示忽略此版本的选项 默认开启
    private boolean canIgnoreThisVersion = true;
    //app图标
    private int iconRes;
    //appName
    private String appName;
    //是否开启日志输出
    private boolean showLog = true;
    //自定义Bean类
    private Object cls;
    //设置请求方式
    private int requestMethod = Builder.METHOD_POST;
    //判断是否是自动检查更新还是用户手动检查的
    private boolean autoCheck;

    //启动service
    private static Intent intent;
    //单例
    private static AutoUpdateUtils sInstance = new AutoUpdateUtils();

    //私有化构造方法
    private AutoUpdateUtils() {

    }

    public void check(Context context, boolean autoCheck) {
        this.autoCheck = autoCheck;
        check(context);
    }

    /**
     * 检查更新不能是
     *
     * @param context 不能是applicationcontext
     */
    public void check(Context context) {
        if (TextUtils.isEmpty(checkUrl)) {
            throw new RuntimeException("checkUrl is null. You must call init before using the library.");
        } else {
            mContext = context;
            appContext = context.getApplicationContext();
            requestPermission(null);
            new DownDataAsyncTask().execute();
        }
    }

    /**
     * 初始化url
     *
     * @param builder
     */
    public void init(Builder builder) {
        checkUrl = builder.baseUrl;
        showType = builder.showType;
        canIgnoreThisVersion = builder.canIgnoreThisVersion;
        iconRes = builder.iconRes;
        appName = builder.appName;
        showLog = builder.showLog;
        requestMethod = builder.requestMethod;
        cls = builder.cls;
    }

    /**
     * getInstance()
     *
     * @return
     */
    public static AutoUpdateUtils getInstance() {
        return sInstance;
    }

    /**
     * 移除引用
     */
    public void detach() {
        mContext = null;
    }

    /**
     * mainactivity调用
     */
    public void destroy() {
        //不要忘了这一步
        if (appContext != null && intent != null) {
            appContext.stopService(intent);
        }
        if (appContext != null && receiver != null) {
//            mContext.unregisterReceiver(receiver);
            LocalBroadcastManager.getInstance(appContext).unregisterReceiver(receiver);
        }

        appContext = null;
        mContext = null;
    }

    /**
     * 异步任务下载数据
     */
    private class DownDataAsyncTask extends AsyncTask<String, Void, UpdateEntity> {

        @Override
        protected UpdateEntity doInBackground(String... params) {
            HttpURLConnection httpURLConnection = null;
            InputStream is;
            StringBuilder sb = new StringBuilder();
            try {
                //准备请求的网络地址
                URL url = new URL(checkUrl);
                //调用openConnection得到网络连接，网络连接处于就绪状态
                httpURLConnection = (HttpURLConnection) url.openConnection();
                //设置网络连接超时时间5S
                httpURLConnection.setConnectTimeout(5 * 1000);
                //设置读取超时时间
                httpURLConnection.setReadTimeout(5 * 1000);
                if (requestMethod == Builder.METHOD_POST) {
                    httpURLConnection.setRequestMethod("POST");
                } else {
                    httpURLConnection.setRequestMethod("GET");
                }
                httpURLConnection.connect();

                //if连接请求码成功
                if (httpURLConnection.getResponseCode() == httpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    byte[] bytes = new byte[1024];
                    int i = 0;
                    while ((i = is.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, i, "utf-8"));
                    }
                    is.close();
                }
                if (showLog) {
                    if (TextUtils.isEmpty(sb.toString())) {
                        Log.e("autoupdatelibrary", "自动更新library返回的数据为空，" +
                                "请检查请求方法是否设置正确，默认为post请求，再检查地址是否输入有误");
                    } else {
                        Log.e("autoupdatelibrary", "自动更新library返回的数据：" + sb.toString());
                    }
                }

                if (cls != null) {
                    if (cls instanceof LibraryUpdateEntity) {
                        LibraryUpdateEntity o = (LibraryUpdateEntity)
                                JSONHelper.parseObject(sb.toString(), cls.getClass());//反序列化

                        UpdateEntity updateEntity = new UpdateEntity();
                        updateEntity.setVersionCode(o.getVersionCodes());
                        updateEntity.setIsForceUpdate(o.getIsForceUpdates());
                        updateEntity.setPreBaselineCode(o.getPreBaselineCodes());
                        updateEntity.setVersionName(o.getVersionNames());
                        updateEntity.setDownurl(o.getDownurls());
                        updateEntity.setUpdateLog(o.getUpdateLogs());
                        updateEntity.setSize(o.getApkSizes());
                        updateEntity.setHasAffectCodes(o.getHasAffectCodess());
                        return updateEntity;
                    } else {
                        throw new RuntimeException("未实现接口：" +
                                cls.getClass().getName() + "未实现LibraryUpdateEntity接口");
                    }
                }
                return JSONHelper.parseObject(sb.toString(), UpdateEntity.class);//反序列化
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException("json解析错误，" +
                        "请按照library中的UpdateEntity所需参数返回数据，json必须包含UpdateEntity所需全部字段");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(UpdateEntity data) {
            super.onPostExecute(data);
            if (data != null) {
                if (data.versionCode > getVersionCode(mContext)) {//不是最新版本
                    if (data.isForceUpdate == 2) {
                        //所有旧版本强制更新
                        showUpdateDialog(data, true, false);
                    } else if (data.isForceUpdate == 1) {
                        //hasAffectCodes提及的版本强制更新
                        String[] hasAffectCodes = data.hasAffectCodes.split("\\|");
                        if (Arrays.asList(hasAffectCodes).contains(ApkUtils.getVersionCode(mContext) + "")) {
                            //被列入强制更新 不可忽略此版本
                            showUpdateDialog(data, true, false);
                        } else {
                            String dataVersion = data.versionName;
                            if (!TextUtils.isEmpty(dataVersion)) {
                                List<String> listCodes = loadArray();
                                if (!listCodes.contains(dataVersion)) {
                                    //没有设置为已忽略
                                    showUpdateDialog(data, false, true);
                                }
                            }
                        }
                    } else if (data.isForceUpdate == 0) {
                        showUpdateDialog(data, false, true);
                    }
                } else {//已是最新版本
                    if (autoCheck) {//系统自动检测不弹提示
                        autoCheck = false;
                    } else {
                        Toast.makeText(mContext, "当前版本已经是最新的了", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(mContext, "网络异常", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 显示更新对话框
     *
     * @param data
     */
    private void showUpdateDialog(final UpdateEntity data, boolean isForceUpdate, boolean showIgnore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.dialog);
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_update_notice, null);
        TextView current_version = (TextView) view.findViewById(R.id.current_version);
        TextView latest_version = (TextView) view.findViewById(R.id.latest_version);
        TextView message = (TextView) view.findViewById(R.id.message);
        TextView update = (TextView) view.findViewById(R.id.update);
        TextView ignore = (TextView) view.findViewById(R.id.ignore);

        builder.setView(view);
        final AlertDialog alertDialog = builder.create();

        String currentVersionName = "0.0.0";
        PackageInfo packInfo = ApkUtils.getPackInfo(mContext);
        if (packInfo != null) {
            currentVersionName = packInfo.versionName;
        }
        current_version.setText("当前版本V" + currentVersionName);

        String latesetVersionName = data.versionName;
        if (TextUtils.isEmpty(latesetVersionName)) {
            latesetVersionName = "1.1";
        }
        latest_version.setText("最新版本V" + latesetVersionName);

        String updateLog = data.updateLog;
        if (TextUtils.isEmpty(updateLog)) {
            updateLog = "新版本，欢迎更新";
        }
        message.setText(updateLog);

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpdate(data);
                alertDialog.dismiss();
            }
        });

        if (canIgnoreThisVersion && showIgnore) {
            final String finalVersionName = latesetVersionName;
            ignore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //忽略此版本
                    List<String> listCodes = loadArray();
                    if (listCodes != null) {
                        listCodes.add(finalVersionName);
                    } else {
                        listCodes = new ArrayList<>();
                        listCodes.add(finalVersionName);
                    }
                    saveArray(listCodes);
                }
            });
        } else {
            ignore.setVisibility(View.GONE);
        }

        if (isForceUpdate) {
            alertDialog.setCancelable(false);
        }

        alertDialog.show();
    }

    @TargetApi(M)
    private void requestPermission(final UpdateEntity data) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 第一次请求权限时，用户如果拒绝，下一次请求shouldShowRequestPermissionRationale()返回true
            // 向用户解释为什么需要这个权限
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(mContext)
                        .setMessage("申请存储权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //申请相机权限
                                ActivityCompat.requestPermissions((Activity) mContext,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSON_REQUEST_CODE);
                            }
                        })
                        .show();
            } else {
                //申请相机权限
                ActivityCompat.requestPermissions((Activity) mContext,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSON_REQUEST_CODE);
            }
        } else {
            if (data != null) {
                if (!TextUtils.isEmpty(data.downurl)) {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        try {
                            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
//                            final String fileName = filePath + "/" + getPackgeName(mContext) + "-v" + getVersionName(mContext) + ".apk";

                            String suffix = data.downurl.substring(data.downurl.lastIndexOf(".")).toLowerCase();
                            final String fileName = filePath + "/" + mContext.getPackageName() + "-v" + ApkUtils.getVersionName(mContext) + suffix;

                            final File file = new File(fileName);
                            //如果不存在
                            if (!file.exists()) {
                                //检测当前网络状态
                                if (!NetWorkUtils.getCurrentNetType(mContext).equals("wifi")) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                    builder.setTitle("提示");
                                    builder.setMessage("当前处于非WIFI连接，是否继续？");
                                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            createFileAndDownload(file, data.downurl);
                                        }
                                    });
                                    builder.setNegativeButton("取消", null);
                                    builder.show();
                                } else {
                                    createFileAndDownload(file, data.downurl);
                                }
                            } else {
                                if (file.length() == Long.parseLong(data.size)) {
//                                    installApkFile(mContext, file);
                                    UpdateCompat.update(mContext, file);
                                } else {
                                    if (!NetWorkUtils.getCurrentNetType(mContext).equals("wifi")) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                        builder.setTitle("提示");
                                        builder.setMessage("当前处于非WIFI连接，是否继续？");
                                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                file.delete();
                                                createFileAndDownload(file, data.downurl);
                                            }
                                        });
                                        builder.setNegativeButton("取消", null);
                                        builder.show();
                                    } else {
                                        file.delete();
                                        createFileAndDownload(file, data.downurl);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(mContext, "没有挂载的SD卡", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mContext, "下载路径为空", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //创建文件并下载文件
    private void createFileAndDownload(File file, String downurl) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            if (!file.createNewFile()) {
                Toast.makeText(mContext, "文件创建失败", Toast.LENGTH_SHORT).show();
            } else {
                //文件创建成功
                intent = new Intent(mContext, DownloadService.class);
                intent.putExtra("downUrl", downurl);
                intent.putExtra("appName", mContext.getString(R.string.app_name));
                intent.putExtra("type", showType);
                if (iconRes != 0) {
                    intent.putExtra("icRes", iconRes);
                }
                mContext.startService(intent);

                //此时才注册receiver
                receiver = new MyReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.MY_RECEIVER");
//                mContext.registerReceiver(receiver, filter);
                LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);

                //显示dialog
                if (showType == Builder.TYPE_DIALOG) {
                    progressDialog = new ProgressDialog(mContext);
                    if (iconRes != 0) {
                        progressDialog.setIcon(iconRes);
                    } else {
                        progressDialog.setIcon(R.mipmap.ic_launcher);
                    }
                    progressDialog.setTitle("正在更新...");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//设置进度条对话框//样式（水平，旋转）
                    //进度最大值
                    progressDialog.setMax(100);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始更新操作
     */
    public void startUpdate(UpdateEntity data) {
        requestPermission(data);
    }

    /**
     * 广播接收器
     *
     * @author user
     */
    private class MyReceiver extends DownloadReceiver {
        @Override
        protected void downloadComplete() {
            if (mContext != null && intent != null) {
                mContext.stopService(intent);
            }
            if (mContext != null && receiver != null) {
//                mContext.unregisterReceiver(receiver);
                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void downloading(int progress) {
            if (progressDialog != null) {
                progressDialog.setProgress(progress);
            }
        }

        @Override
        protected void downloadFail(String e) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
        }
    }

    //建造者模式
    public static final class Builder {
        private String baseUrl;
        private int showType = TYPE_DIALOG;
        //是否显示忽略此版本 true 是 false 否
        private boolean canIgnoreThisVersion = true;
        //在通知栏显示进度
        public static final int TYPE_NITIFICATION = 1;
        //对话框显示进度
        public static final int TYPE_DIALOG = 2;
        //POST方法
        public static final int METHOD_POST = 3;
        //GET方法
        public static final int METHOD_GET = 4;
        //显示的app资源图
        private int iconRes;
        //显示的app名
        private String appName;
        //显示log日志
        private boolean showLog;
        //设置请求方式
        private int requestMethod = METHOD_POST;
        //自定义Bean类
        private Object cls;

        public final AutoUpdateUtils.Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public final AutoUpdateUtils.Builder setTransition(Object cls) {
            this.cls = cls;
            return this;
        }

        public final AutoUpdateUtils.Builder showLog(boolean showLog) {
            this.showLog = showLog;
            return this;
        }

        public final AutoUpdateUtils.Builder setRequestMethod(int requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public final AutoUpdateUtils.Builder setShowType(int showType) {
            this.showType = showType;
            return this;
        }

        public final AutoUpdateUtils.Builder setIconRes(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public final AutoUpdateUtils.Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public final AutoUpdateUtils.Builder setIgnoreThisVersion(boolean canIgnoreThisVersion) {
            this.canIgnoreThisVersion = canIgnoreThisVersion;
            return this;
        }

        public final Builder build() {
            return this;
        }
    }

    private boolean saveArray(List<String> list) {
        SharedPreferences sp = mContext.getSharedPreferences("ingoreList", mContext.MODE_PRIVATE);
        SharedPreferences.Editor mEdit1 = sp.edit();
        mEdit1.putInt("Status_size", list.size());

        for (int i = 0; i < list.size(); i++) {
            mEdit1.remove("Status_" + i);
            mEdit1.putString("Status_" + i, list.get(i));
        }
        return mEdit1.commit();
    }

    private List<String> loadArray() {
        List<String> list = new ArrayList<>();
        SharedPreferences mSharedPreference1 = mContext.getSharedPreferences("ingoreList", mContext.MODE_PRIVATE);
        list.clear();
        int size = mSharedPreference1.getInt("Status_size", 0);
        for (int i = 0; i < size; i++) {
            list.add(mSharedPreference1.getString("Status_" + i, null));
        }
        return list;
    }
}
