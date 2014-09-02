package com.mobilesolutionworks.android.httpcache;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by yunarta on 3/9/14.
 */
public class HttpTagConfiguration {

    protected static HttpTagConfiguration INSTANCE;

    public static HttpTagConfiguration configure(Context context) {
        if (INSTANCE == null) {
            Context applicationContext = context.getApplicationContext();

            Bundle metaData;
            try {
                ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                metaData = ai.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                metaData = new Bundle();
            }

            if (!metaData.containsKey("works.httpcache.service") || !metaData.containsKey("works.httpcache.authority")) {
                throw new IllegalStateException("works.httpcache.service OR works.httpcache.authority not configured properly");
            }

            INSTANCE = new HttpTagConfiguration(
                    metaData.getString("works.httpcache.service"),
                    new Uri.Builder().scheme("content").authority(metaData.getString("works.httpcache.authority")).build()
            );
        }

        return INSTANCE;
    }

    public final Uri authority;

    public final String action;

    protected HttpTagConfiguration(String action, Uri authority) {
        this.action = action;
        this.authority = authority;
    }
}
