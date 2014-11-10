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
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mobilesolutionworks.android.http.WorksHttpFutureTask;
import com.mobilesolutionworks.android.http.WorksHttpRequest;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yunarta on 24/8/14.
 */
public class WorksHttpCacheService extends HttpCacheService {

    private ExecutorService mExecutors;

    @Override
    public void onCreate() {
        super.onCreate();

        mExecutors = Executors.newCachedThreadPool();
    }

    @Override
    protected void executeRequest(String local, String remote, String method, Bundle params, int cache, int timeout) {
        WorksHttpRequest.Method _method = WorksHttpRequest.Method.GET;
        if ("POST".equals(method)) {
            _method = WorksHttpRequest.Method.POST;
        }

        WorksHttpRequest config = new WorksHttpRequest();
        config.method = _method;
        config.url = remote;

        if (params != null) {
            for (String key : params.keySet()) {
                String value = params.getString(key);
                if (!TextUtils.isEmpty(value)) {
                    config.setPostParam(key, value);
                }
            }
        }

        WorksHttpFutureTask<String> task = getSaveTask(local, cache, timeout);
        task.execute(config, mHandler, mExecutors);
    }

    protected HttpContext onGetHttpContext() {
        return null;
    }

    protected class QueryAndSaveTask extends WorksHttpFutureTask<String> {

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
        public HttpContext getHttpContext() {
            return onGetHttpContext();
        }

        @Override
        public void onPreExecute(WorksHttpRequest request, HttpUriRequest httpRequest) {
            super.onPreExecute(request, httpRequest);
            WorksHttpCacheService.this.onPreExecute(request, httpRequest);

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, mTimeout);
            HttpConnectionParams.setSoTimeout(params, mTimeout);
        }

        @Override
        public boolean onValidateResponse(WorksHttpRequest request, HttpResponse httpResponse) {
            StatusLine statusLine = httpResponse.getStatusLine();
            return (statusLine.getStatusCode() >= 200) && (statusLine.getStatusCode() < 300) || WorksHttpCacheService.this.onValidateResponse(request, httpResponse);
        }

        @Override
        public void onLoadFinished(WorksHttpRequest request, int statusCode, String data) {
            ContentValues values = new ContentValues();
            values.put("local", mLocalUri);
            values.put("remote", request.url);
            values.put("data", data);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.OK);


            insert(values, mLocalUri);
        }

        @Override
        public void onProcessError(WorksHttpRequest request, Throwable exception) {
            super.onProcessError(request, exception);

            ContentValues values = new ContentValues();
            values.put("local", mLocalUri);
            values.put("remote", request.url);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.PROCESS_ERROR);

            onReportError(request, exception);

            insert(values, mLocalUri);
        }

        @Override
        public void onNetError(WorksHttpRequest request, int statusCode) {
            super.onNetError(request, statusCode);

            ContentValues values = new ContentValues();
            values.put("local", mLocalUri);
            values.put("remote", request.url);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.createNet(statusCode));

            insert(values, mLocalUri);
        }

        @Override
        public void onCancelled(WorksHttpRequest request) {
            super.onCancelled(request);

            ContentValues values = new ContentValues();
            values.put("local", mLocalUri);
            values.put("remote", request.url);
            values.put("time", System.currentTimeMillis() + mCache);
            values.put("error", CacheErrorCode.CANCELED);

            insert(values, mLocalUri);
        }
    }


    protected WorksHttpFutureTask<String> getSaveTask(String local, int cache, int timeout) {
        return new QueryAndSaveTask(this, local, cache, timeout);
    }

    protected void onReportError(WorksHttpRequest request, Throwable exception) {

    }

    protected boolean onValidateResponse(WorksHttpRequest request, HttpResponse response) {
        return true;
    }

    protected void onPreExecute(WorksHttpRequest request, HttpUriRequest httpRequest) {

    }
}
