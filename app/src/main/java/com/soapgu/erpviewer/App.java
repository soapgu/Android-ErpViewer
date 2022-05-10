package com.soapgu.erpviewer;

import android.app.Application;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

public class App extends Application {
    final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
            Thread.getDefaultUncaughtExceptionHandler();

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            Logger.e( paramThrowable, "----------UncaughtException throw---------" );
            if( defaultUncaughtExceptionHandler != null ){
                defaultUncaughtExceptionHandler.uncaughtException( paramThread,paramThrowable );
            }
        });

        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)
                .tag("erp")
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
        int pid = android.os.Process.myPid();
        Logger.i( "~~~~~~~erp app startup,pid:%s~~~~~~~~", pid );
    }
}
