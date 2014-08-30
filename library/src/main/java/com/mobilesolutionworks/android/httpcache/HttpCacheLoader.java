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


import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import java.util.Set;

/**
 * Created by yunarta on 31/7/14.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class HttpCacheLoader extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

    protected String[] mArgs;

    protected Context mContext;

    protected Cursor mCursor;

    protected final Uri mUri;

    protected final String mParams;

    boolean mRegistered;

    public HttpCacheLoader(Context context, Uri uri) {
        super(new Handler());

        mContext = context;
        mUri = uri;
        mParams = "{}";
    }

    public HttpCacheLoader(Context context, Uri uri, String params) {
        super(new Handler());

        mContext = context;
        mUri = uri;
        mParams = params;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String cache = mUri.getQueryParameter("cache");

        if (!TextUtils.isEmpty(cache)) {
            if ("0".equals(cache)) {
                ContentValues values = new ContentValues();
                values.put("error", CacheErrorCode.DELETED.value());

                Uri.Builder builder = mUri.buildUpon();
                builder.clearQuery();

                Set<String> names = mUri.getQueryParameterNames();
                for (String name : names) {
                    if ("cache".equals(name) || "timeout".equals(name) || "test".equals(name)) {
                        continue;
                    }

                    builder.appendQueryParameter(name, mUri.getQueryParameter(name));
                }

                Uri mUriPathOnly = builder.build();
                mContext.getContentResolver().update(mUriPathOnly, values, null, null);
            }
        }

        Uri uri = mUri.buildUpon().appendQueryParameter("test", "1").build();
        return new SyncCursorLoader(mContext, uri, new String[]{"data", "time", "error"}, mParams, mArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mCursor != null) {
            if (mRegistered) {
                mCursor.unregisterContentObserver(this);
                mRegistered = false;
            }
        }

        boolean deliver = false;
        if (mCursor == null && cursor != null) {
            deliver = true;
        }

        mCursor = cursor;
        if (mCursor == null) {
            nodata();
        } else {
            if (!mRegistered) {
                mCursor.registerContentObserver(this);
                mRegistered = true;
            }

            Uri.Builder builder = mUri.buildUpon();
            builder.clearQuery();

            Set<String> names = mUri.getQueryParameterNames();
            for (String name : names) {
                if ("cache".equals(name) || "timeout".equals(name) || "test".equals(name)) {
                    continue;
                }

                builder.appendQueryParameter(name, mUri.getQueryParameter(name));
            }

            Uri mUriPathOnly = builder.build();
            mCursor.setNotificationUri(mContext.getContentResolver(), mUriPathOnly);

            if (mCursor.moveToFirst()) {
                String data = mCursor.getString(mCursor.getColumnIndex("data"));
                long time = mCursor.getLong(mCursor.getColumnIndex("time"));

                int error = mCursor.getInt(mCursor.getColumnIndex("error"));
                if (deliver) {
                    beforeUse(error, data, time, false);
                }
            } else {
                nodata();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mCursor != null) {
            if (mRegistered) {
                mCursor.unregisterContentObserver(this);
                mRegistered = false;
            }
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        if (mCursor != null) {
            if (mRegistered) {
                mCursor.unregisterContentObserver(this);
                mRegistered = false;
            }
            mCursor.close();
        }

        mCursor = mContext.getContentResolver().query(mUri, new String[]{"data", "time", "error"}, mParams, mArgs, null);
        mCursor.registerContentObserver(this);
        mRegistered = true;

        Uri.Builder builder = mUri.buildUpon();
        Set<String> names = mUri.getQueryParameterNames();
        for (String name : names) {
            if ("cache".equals(name) || "timeout".equals(name) || "test".equals(name)) {
                continue;
            }

            builder.appendQueryParameter(name, mUri.getQueryParameter(name));
        }

        Uri mUriPathOnly = builder.build();
        mCursor.setNotificationUri(mContext.getContentResolver(), mUriPathOnly);

        // mCursor.requery();
        if (!mCursor.isClosed() && mCursor.moveToFirst()) {
            String data = mCursor.getString(mCursor.getColumnIndex("data"));
            long time = mCursor.getLong(mCursor.getColumnIndex("time"));
            int error = mCursor.getInt(mCursor.getColumnIndex("error"));

            beforeUse(error, data, time, true);
        }
    }

    protected void stopMonitor() {
        if (mRegistered) {
            mCursor.unregisterContentObserver(this);
            mRegistered = false;
        }
    }

    private void beforeUse(int errorCode, String data, long time, boolean requery) {
        try {
            CacheErrorCode generic = CacheErrorCode.getGeneric(errorCode);
            switch (generic) {
                case GENERIC_NET_ERROR: {
                    if (netf(errorCode, data)) {
                        return;
                    }
                    break;
                }

                case GENERIC_PROCESS_ERROR: {
                    if (pf(errorCode, data)) {
                        return;
                    }
                    break;
                }

                default: {
                    use(errorCode, data, time);
                    return;
                }
            }

            error(errorCode, data);

//            if (!requery)
//            {
//                ContentValues values = new ContentValues();
//                values.put("error", CacheErrorCode.DELETED.value());
//
//                Uri mUriPathOnly = mUri.buildUpon().clearQuery().build();
//                mContext.getContentResolver().update(mUriPathOnly, values, null, null);
//            }
        } finally {
            completed();
        }
    }

    protected boolean pf(int error, String data) {
        return false;
    }

    protected boolean netf(int error, String data) {
        return false;
    }

    protected abstract void nodata();

    protected abstract void use(int error, String data, long time);

    protected abstract void error(int error, String data);

    protected void completed() {

    }
}

