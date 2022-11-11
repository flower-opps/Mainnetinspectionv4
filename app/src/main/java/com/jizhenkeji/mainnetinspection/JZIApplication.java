package com.jizhenkeji.mainnetinspection;

import android.app.Application;
import android.content.Context;

import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.secneo.sdk.Helper;

public class JZIApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        /* 在任何SDK功能使用前进行加载，用于对SDK类进行加载 */
        Helper.install(JZIApplication.this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalUtils.initialize(getApplicationContext());
    }

}
