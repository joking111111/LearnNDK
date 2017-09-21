package com.joking.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.joking.autoupdate.utils.AutoUpdateUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //表示如果是最新版就不会弹出已经是最新版的提示
        AutoUpdateUtils.getInstance().check(this, true);
        //检测更新按钮等需要弹已经最新版的提示
        AutoUpdateUtils.getInstance().check(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AutoUpdateUtils.getInstance().destroy();
    }
}
