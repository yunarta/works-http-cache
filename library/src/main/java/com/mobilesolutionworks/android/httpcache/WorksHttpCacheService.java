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
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mobilesolutionworks.android.http.WorksHttpAsyncTask;
import com.mobilesolutionworks.android.http.WorksHttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yunarta on 31/7/14.
 */
public abstract class WorksHttpCacheService extends IntentService {

    protected Set<Uri> mQueues;

    protected String mGetDataIntent;

    protected Gson mGson;

    protected ExecutorService mExecutors;

    public WorksHttpCacheService() {
        super("cache-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mQueues = new HashSet<Uri>();
        mExecutors = Executors.newCachedThreadPool();

        Bundle metaData;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            metaData = new Bundle();
            metaData.putString("getDataIntent", "GET_DATA_INTENT");
        }

        mGetDataIntent = metaData.getString("getDataIntent");
        mGson = new GsonBuilder().create();
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

    protected abstract String createUrl(Uri data, String[] args);

    protected abstract int resolveUri(Uri data);

    protected abstract Uri createUri(String path);

    protected void refreshData(Intent intent) {
        Uri data = Uri.parse(intent.getData().toString());

        Log.d("WorksHttpCache", "going to fetch for " + data);
        if (resolveUri(data) == -1) {
            return;
        }

        if (mQueues.contains(data)) {
            return;
        }

        mQueues.add(data);

        String[] args = intent.getStringArrayExtra("args");
        if (args == null) {
            args = new String[0];
        }

        WorksHttpRequest config = new WorksHttpRequest();
        config.method = WorksHttpRequest.Method.POST;
        config.url = createUrl(data, args);

        String params = intent.getStringExtra("params");
        if (!TextUtils.isEmpty(params)) {
            JsonObject parse = mGson.fromJson(params, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : parse.entrySet()) {
                String value = entry.getValue().getAsString();
                if (!TextUtils.isEmpty(value)) {
                    config.setPostParam(entry.getKey(), value);
                }
            }
        }

        String cache = data.getQueryParameter("cache");
        long time = 2 * 60;
        if (!TextUtils.isEmpty(cache)) {
            time = Long.parseLong(cache);
            if (time == 0) {
                time = 3 * 60;
            }

        }
        time *= 1000;

        String timeout = data.getQueryParameter("timeout");
        int lTimeout = 10;
        if (!TextUtils.isEmpty(timeout)) {
            lTimeout = Integer.parseInt(timeout);
            if (lTimeout == 0) {
                lTimeout = 10;
            }

        }
        lTimeout *= 1000;

        Set<String> names = data.getQueryParameterNames();

        Uri.Builder builder = data.buildUpon();
        builder.clearQuery();
        for (String name : names) {
            if ("cache".equals(name) || "timeout".equals(name)) {
                continue;
            }

            builder.appendQueryParameter(name, data.getQueryParameter(name));
        }

        Uri build = builder.build();
        String path = build.getEncodedPath();
        if (build.getEncodedQuery() != null) {
            path += "?" + build.getEncodedQuery();
        }

        QueryAndSaveTask task = new QueryAndSaveTask(this, path, intent.getData(), time, lTimeout);
        task.executeOnExecutor(mExecutors, config);
    }

    private class QueryAndSaveTask extends WorksHttpAsyncTask<String> {

        private String mPath;

        private final long mTime;

        private Uri mContentUri;

        private int mTimeout;

        public QueryAndSaveTask(Context context, String path, Uri uri, long time, int timeout) {
            super(context);

            mPath = path;
            mTime = time;
            mContentUri = uri;
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
            values.put("uri", mPath);
            values.put("data", data);
            values.put("time", System.currentTimeMillis() + mTime);
            values.put("error", CacheErrorCode.OK.value());

            Uri uri = createUri(mPath);
            mQueues.remove(mContentUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(mContentUri, null);
        }

        @Override
        public void onProcessError(WorksHttpRequest request, Throwable exception) {
            super.onProcessError(request, exception);

            ContentValues values = new ContentValues();
            values.put("uri", mPath);
            values.put("time", System.currentTimeMillis() + mTime);
            values.put("error", CacheErrorCode.createException(exception).value());

            Uri uri = createUri(mPath);
            mQueues.remove(mContentUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(mContentUri, null);
        }

        @Override
        public void onNetError(WorksHttpRequest request, int statusCode) {
            super.onNetError(request, statusCode);

            ContentValues values = new ContentValues();
            values.put("uri", mPath);
            values.put("time", System.currentTimeMillis() + mTime);
            values.put("error", CacheErrorCode.createNet(statusCode).value());

            Uri uri = createUri(mPath);
            mQueues.remove(mContentUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(mContentUri, null);
        }

        @Override
        public void onCancelled(WorksHttpRequest request) {
            super.onCancelled(request);

            ContentValues values = new ContentValues();
            values.put("uri", mPath);
            values.put("time", System.currentTimeMillis() + mTime);
            values.put("error", CacheErrorCode.CANCELED.value());

            Uri uri = createUri(mPath);
            mQueues.remove(mContentUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(mContentUri, null);
        }
    }

}
