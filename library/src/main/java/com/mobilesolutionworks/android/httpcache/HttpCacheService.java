/*
 * Copyright 2014-present Yunarta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilesolutionworks.android.httpcache;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.util.HashSet;
import java.util.Set;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;

/**
 * Created by yunarta on 29/10/14.
 */
public class HttpCacheService extends BaseService {

    protected Handler mHandler;

    protected Set<String> mQueues;

    protected HttpCacheConfiguration mConfiguration;

    protected CookieStore mCookieStore;

    public HttpCacheService() {
        super("tag-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mQueues = new HashSet<String>();
        mConfiguration = HttpCacheConfiguration.configure(this);

        mCookieStore = new BasicCookieStore();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (mConfiguration.action.equals(action)) {
                refreshData(intent);
            } else if (mConfiguration.clearCookie.equals(action)) {
                clearCookie();
            }
        }
    }

    protected void refreshData(Intent intent) {
        String local = intent.getStringExtra("local");
        String remote = intent.getStringExtra("remote");

        if (mQueues.contains(local)) {
            return;
        }

        mQueues.add(local);


        String _method = intent.getStringExtra("method");
        Bundle params = intent.getBundleExtra("params");

        int cache = intent.getIntExtra("cache", 0);
        if (cache == 0) {
            cache = 60;
        }

        cache *= 1000;

        int timeout = intent.getIntExtra("timeout", 0);
        if (timeout == 0) {
            timeout = 10;
        }
        timeout *= 1000;

        executeRequest(local, remote, _method, params, cache, timeout);
    }

    protected void clearCookie() {
        mCookieStore.clear();
    }

    protected void executeRequest(String local, String remote, String method, Bundle params, int cache, int timeout) {

    }

    protected void insert(ContentValues values, String localUri) {
        mQueues.remove(localUri);

        Uri uri = Uri.withAppendedPath(mConfiguration.authority, localUri);

        getContentResolver().insert(uri, values);
        getContentResolver().notifyChange(uri, null);
    }

    protected boolean onValidateStatus(int statusCode) {
        return true;
    }
}
