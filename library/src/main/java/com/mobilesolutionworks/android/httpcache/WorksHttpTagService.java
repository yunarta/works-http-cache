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

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import com.mobilesolutionworks.android.http.WorksHttpFutureTask;
import com.mobilesolutionworks.android.http.WorksHttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yunarta on 24/8/14.
 */
public class WorksHttpTagService extends IntentService {

    private Handler mHandler;

    private Set<String> mQueues;

    private ExecutorService mExecutors;

    private String mGetDataIntent;

    private Uri mAuthority;

    public WorksHttpTagService() {
        super("tag-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mQueues = new HashSet<String>();
        mExecutors = Executors.newCachedThreadPool();

        Bundle metaData;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            metaData = new Bundle();
            metaData.putString("getTagIntent", "GET_DATA_INTENT");
        }

        mGetDataIntent = metaData.getString("getTagIntent");
        mAuthority = new Uri.Builder().scheme("content").authority(metaData.getString("tagAuthority")).build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (mGetDataIntent.equals(action)) {
                refreshData(intent);
            }
        }
    }

    protected void refreshData(Intent intent) {
        // rebuild into hierarchical uri
        String local = intent.getStringExtra("local");
        String remote = intent.getStringExtra("remote");

        if (mQueues.contains(local)) {
            return;
        }

        mQueues.add(local);

        String _method = intent.getStringExtra("method");
        WorksHttpRequest.Method method = WorksHttpRequest.Method.GET;
        if ("POST".equals(_method)) {
            method = WorksHttpRequest.Method.POST;
        }

        WorksHttpRequest config = new WorksHttpRequest();
        config.method = method;
        config.url = remote;

        Bundle params = intent.getBundleExtra("params");
        if (params != null) {
            for (String key : params.keySet()) {
                String value = params.getString(key);
                if (!TextUtils.isEmpty(value)) {
                    config.setPostParam(key, value);
                }
            }
        }

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

        QueryAndSaveTask task = new QueryAndSaveTask(this, local, cache, timeout);
        task.execute(config, mHandler, mExecutors);
    }

    private class QueryAndSaveTask extends WorksHttpFutureTask<String> {

        private final long mCache;

        private String mLocalUri;

        private int mTimeout;

        public QueryAndSaveTask(Context context, String local, int cache, int timeout) {
            super(context);

            mLocalUri = local;
            mCache = cache;
            mTimeout = timeout;
        }

        @Override
        public void onPreExecute(WorksHttpRequest request, HttpUriRequest httpRequest) {
            super.onPreExecute(request, httpRequest);

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, mTimeout);
            HttpConnectionParams.setSoTimeout(params, mTimeout);
        }

        @Override
        public boolean onValidateResponse(WorksHttpRequest request, HttpResponse httpResponse) {
            StatusLine statusLine = httpResponse.getStatusLine();
            return (statusLine.getStatusCode() >= 200) && (statusLine.getStatusCode() < 300);
        }

        @Override
        public void onLoadFinished(WorksHttpRequest request, int statusCode, String data) {
            ContentValues values = new ContentValues();
            values.put("local", mLocalUri);
            values.put("remote", request.url);
            values.put("data", data);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.OK.value());

            mQueues.remove(mLocalUri);

            insert(values);
        }

        @Override
        public void onProcessError(WorksHttpRequest request, Throwable exception) {
            super.onProcessError(request, exception);

            ContentValues values = new ContentValues();
            values.put("uri", mLocalUri);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.createException(exception).value());

            mQueues.remove(mLocalUri);

            insert(values);
        }

        @Override
        public void onNetError(WorksHttpRequest request, int statusCode) {
            super.onNetError(request, statusCode);

            ContentValues values = new ContentValues();
            values.put("uri", mLocalUri);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.createNet(statusCode).value());

            mQueues.remove(mLocalUri);

            insert(values);
        }

        @Override
        public void onCancelled(WorksHttpRequest request) {
            super.onCancelled(request);

            ContentValues values = new ContentValues();
            values.put("uri", mLocalUri);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.CANCELED.value());

            mQueues.remove(mLocalUri);

            insert(values);
        }

        private void insert(ContentValues values) {
            Uri uri = Uri.withAppendedPath(mAuthority, mLocalUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(uri, null);
        }

    }
}
