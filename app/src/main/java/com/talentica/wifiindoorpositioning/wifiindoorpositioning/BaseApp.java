package com.talentica.wifiindoorpositioning.wifiindoorpositioning;

import android.app.Application;

/**
 * Created by suyashg on 25/08/17.
 */

public class BaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 这里原来是 Realm.init(this)，现在已经不需要了
        // 如果以后要初始化其他全局组件，可以放在这里
    }
}
