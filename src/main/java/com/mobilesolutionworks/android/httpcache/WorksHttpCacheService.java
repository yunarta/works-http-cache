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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mobilesolutionworks.android.http.WorksHttpAsyncTask;
import com.mobilesolutionworks.android.http.WorksHttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by yunarta on 31/7/14.
 */
public abstract class WorksHttpCacheService extends IntentService
{
    protected Set<Uri> mQueues;

    protected String mGetDataIntent;

    protected Gson mGson;

    public WorksHttpCacheService()
    {
        super("cache-service");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mQueues = new HashSet<Uri>();

        Bundle metaData;
        try
        {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            metaData = new Bundle();
            metaData.putString("getDataIntent", "GET_DATA_INTENT");
        }

        mGetDataIntent = metaData.getString("getDataIntent");
        mGson = new GsonBuilder().create();
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            String action = intent.getAction();
            if (mGetDataIntent.equals(action))
            {
                refreshData(intent);
            }
        }
    }

    protected abstract int resolveUri(Uri data);

    protected abstract Uri createUri(String data);

    protected abstract String buildUri(Uri data);

    protected void refreshData(Intent intent)
    {
        Uri data = Uri.parse(intent.getData().toString());
        int match = resolveUri(data);
        if (mQueues.contains(data))
        {
            return;
        }

        mQueues.add(data);

        WorksHttpRequest config = new WorksHttpRequest();
        config.method = WorksHttpRequest.Method.POST;
        config.url = buildUri(data);

        String params = intent.getStringExtra("params");
        if (!TextUtils.isEmpty(params))
        {
            JsonObject parse = mGson.fromJson(params, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : parse.entrySet())
            {
                config.setPostParam(entry.getKey(), entry.getValue().getAsString());
            }
        }

        String cache = data.getQueryParameter("cache");
        long time = 2 * 60;
        if (!TextUtils.isEmpty(cache))
        {
            time = Long.parseLong(cache);
            if (time == 0)
            {
                time = 3 * 60;
            }

        }
        time *= 1000;

        QueryAndSaveTask task = new QueryAndSaveTask(this, data.getEncodedPath(), intent.getData(), time);
        task.execute(config);
    }

    private class QueryAndSaveTask extends WorksHttpAsyncTask<String>
    {
        private String mPath;

        private final long mTime;

        private Uri mContentUri;

        @Override
        public void onPreExecute(WorksHttpRequest request, HttpUriRequest httpRequest)
        {
            super.onPreExecute(request, httpRequest);
        }

        public QueryAndSaveTask(Context context, String path, Uri uri, long time)
        {
            super(context);

            mPath = path;
            mTime = time;
            mContentUri = uri;
        }

        @Override
        public boolean onValidateResponse(WorksHttpRequest request, HttpResponse httpResponse)
        {
            StatusLine statusLine = httpResponse.getStatusLine();
            return (statusLine.getStatusCode() >= 200) && (statusLine.getStatusCode() < 300);
        }

        @Override
        public void onLoadFinished(WorksHttpRequest request, int statusCode, String data)
        {
//            GsonInstance instance = GsonInstance.getInstance();
//            RESTResponse response = instance.parse(data, RESTResponse.class);
//            if (response.error.value() == 0)
            {
                ContentValues values = new ContentValues();
                values.put("uri", mPath);
                values.put("json", data);
                values.put("time", System.currentTimeMillis() + mTime);
                values.put("error", CacheErrorCode.OK.value());

                Uri uri = createUri(mPath);
                mQueues.remove(mContentUri);

                getContentResolver().insert(uri, values);
                getContentResolver().notifyChange(mContentUri, null);
            }
        }

        @Override
        public void onProcessError(WorksHttpRequest request, Throwable exception)
        {
            super.onProcessError(request, exception);

            ContentValues values = new ContentValues();
            values.put("uri", mPath);
            values.put("time", System.currentTimeMillis() + mTime);
            values.put("error", CacheErrorCode.GENERIC_PROCESS_ERROR.value());

            Uri uri = createUri(mPath);
            mQueues.remove(mContentUri);

            getContentResolver().insert(uri, values);
            getContentResolver().notifyChange(mContentUri, null);
        }

        @Override
        public void onNetError(WorksHttpRequest request, int statusCode)
        {
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
        public void onCancelled(WorksHttpRequest request)
        {
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
