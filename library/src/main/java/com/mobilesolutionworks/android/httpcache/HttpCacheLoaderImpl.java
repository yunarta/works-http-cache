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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;

import org.apache.commons.lang3.SerializationUtils;

/**
 * Created by yunarta on 24/8/14.
 */
public class HttpCacheLoaderImpl {

    public interface Callback {

        boolean willDispatch(HttpCacheRequest builder);
    }

    private static final String[] PROJECTION = new String[]{"remote", "data", "time", "error", "trace", "status"};

    HttpCacheRequest mBuilder;

    Context mContext;

    HttpCache mTag;

    Callback mCallback;

    public HttpCacheLoaderImpl(Context context, HttpCacheRequest builder, Callback callback)
    {
        mContext = context;
        mBuilder = builder;
        mCallback = callback;
    }

    public HttpCache onForceLoad(ContentObserver observer)
    {
        HttpCache tag = new HttpCache();
        tag.local = mBuilder.localUri();

        Uri authority = HttpCacheConfiguration.configure(mContext).authority;

        ContentResolver cr = mContext.getContentResolver();
        tag.cursor = cr.query(authority, PROJECTION, "local = ?", new String[]{tag.local}, null);
        if (tag.cursor == null)
        {
            // cursor only null if provider is not set
            throw new IllegalStateException("is tag provider set properly?");
        }

        tag.cursor.getCount();
        tag.cursor.registerContentObserver(observer);
        tag.cursor.setNotificationUri(mContext.getContentResolver(), authority.buildUpon().appendEncodedPath(tag.local).build());

        if (tag.cursor.moveToFirst())
        {
            // cache stored in database
            tag.loaded = true;
            tag.remote = tag.cursor.getString(0);
            tag.content = tag.cursor.getString(1);
            tag.expiry = tag.cursor.getLong(2);
            tag.error = tag.cursor.getInt(3);

            byte[] trace = tag.cursor.getBlob(4);
            if (trace != null) {
                tag.trace = SerializationUtils.deserialize(trace);
            }

            tag.status = tag.cursor.getInt(5);
        }

        return tag;
    }

    public boolean deliverResult(HttpCache tag) {
        boolean contentChanged = mTag != null;
        if (mTag != null && mTag != tag) {
            mTag.close();
        }

        mTag = tag;

        boolean noCache = mBuilder.isNoCache() && !contentChanged;
        boolean dispatchRequest = noCache;
        boolean deliverResult = false;

        if (tag.loaded) {
            if (contentChanged) {
                // content change indicating that service had returned the data
                mTag.loadFinished = true;
                deliverResult = true;
                dispatchRequest = false;
            } else {
                // new request
                if (mTag.error != 0 || // current cache is invalid
                    !mTag.remote.equals(mBuilder.remoteUri()) || // remote uri changed
                    mTag.expiry < System.currentTimeMillis() ||  // data expired
                    (mBuilder.keepFresh() && mTag.expiry - System.currentTimeMillis() > mBuilder.cacheExpiry() * 1000) // new expiry timing
                    ) {
                    dispatchRequest = true;
                }

                deliverResult = !dispatchRequest;

                if (mTag.error == 0 && mBuilder.isLoadCacheAnyway()) {
                    deliverResult = true;
                    mTag.loadFinished = false;
                }
            }


//            if (mTag.error == 0 || contentChanged) {
//                if ((mBuilder.isLoadCacheAnyway() && !noCache)) {
//                    dispatchRequest = true;
//                }
//
//                deliverResult = !dispatchRequest;
//                if (mBuilder.isLoadCacheAnyway() && contentChanged) {
//                    deliverResult = true;
//                }
//            }
//
//            if (mTag.error != 0 && !contentChanged) {
//                dispatchRequest = true;
//            }
        } else {
            dispatchRequest = true;
        }

        if (dispatchRequest) {
            if (!mCallback.willDispatch(mBuilder)) {
                Intent service = new Intent(HttpCacheConfiguration.configure(mContext).action);

                service.putExtra("local", mBuilder.localUri());
                service.putExtra("remote", mBuilder.remoteUri());
                service.putExtra("cache", mBuilder.cacheExpiry());
                service.putExtra("timeout", mBuilder.timeout());
                service.putExtra("params", mBuilder.params());
                service.putExtra("method", mBuilder.method());
                service.putExtra("token", mBuilder.token());

                mContext.startService(service);
            }
        }

        return deliverResult;
    }

    public void onStopLoading() {
        if (mTag != null) {
            mTag.close();
        }
    }

    public void onReset() {
        mTag = null;
    }

    public void stopChangeNotificaton(ContentObserver observer) {

        if (!mTag.detached) {
            mTag.detached = true;
            mTag.cursor.unregisterContentObserver(observer);
        }
    }
}