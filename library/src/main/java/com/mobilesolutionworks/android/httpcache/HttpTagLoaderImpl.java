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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by yunarta on 24/8/14.
 */
public class HttpTagLoaderImpl {

    private final HttpTagBuilder mBuilder;

    private final String mGetTagIntent;

    private final Uri mAuthority;

    private static final String[] PROJECTION = new String[]{"remote", "data", "time", "error"};

    Uri mUri;

    String[] mProjection;

    String mSelection;

    String[] mSelectionArgs;

    String mSortOrder;

    Context mContext;

    HttpTag mTag;

    public HttpTagLoaderImpl(Context context, HttpTagBuilder builder) {
        mContext = context;
        builder = builder;

        Bundle metaData;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            metaData = new Bundle();
            metaData.putString("getTagIntent", "GET_TAG_INTENT");
        }

        mGetTagIntent = metaData.getString("getTagIntent");
        mAuthority = new Uri.Builder().scheme("content").authority(metaData.getString("tagAuthority")).build();

        mBuilder = builder;
    }

    public HttpTag onForceLoad(ContentObserver observer) {
        HttpTag tag = new HttpTag();
        tag.local = mBuilder.localUri();


        String remote;
        String data;
        long expiry;
        int error;

        ContentResolver cr = mContext.getContentResolver();
        tag.cursor = cr.query(mAuthority, PROJECTION, "local = ?", new String[]{tag.local}, null);
        if (tag.cursor == null) {
            // cursor only null if provider is not set
            throw new IllegalStateException("is tag provider set properly?");
        }

        tag.cursor.getCount();
        tag.cursor.registerContentObserver(observer);
        tag.cursor.setNotificationUri(mContext.getContentResolver(), mAuthority.buildUpon().appendEncodedPath(tag.local).build());

        if (tag.cursor.moveToFirst()) {
            // cache stored in database
            tag.loaded = true;
            tag.remote = tag.cursor.getString(0);
            tag.content = tag.cursor.getString(1);
            tag.expiry = tag.cursor.getLong(2);
            tag.error = tag.cursor.getInt(3);


//            if (!remote.equals(mBuilder.remoteUri()) || (expiry < System.currentTimeMillis())) {
//                // remote uri is different or data is expired
//                dispatchRequest = true;
//            }
//
//            if (error == 0 && mBuilder.isLoadCacheAnyway()) {
//                // data found without error
//                // we will send the data back to loader as requested
//                data = cursor.getString(1);
//
//                dispatchRequest = true;
//                deliverResult = true;
//            }
        }

        return tag;
    }

    public boolean deliverResult(HttpTag tag) {
        boolean contentChanged = mTag != null;
        if (mTag != null && mTag != tag) {
            mTag.close();
        }

        mTag = tag;

        boolean noCache = mBuilder.isNoCache() && !contentChanged;
        boolean dispatchRequest = noCache;
        boolean deliverResult = false;

        if (tag.loaded) {


            if (!mTag.remote.equals(mBuilder.remoteUri()) || mTag.expiry < System.currentTimeMillis() || mTag.expiry - System.currentTimeMillis() > mBuilder.cacheExpiry() * 1000) {
                dispatchRequest = true;
            }

            if (mTag.error == 0) {
                if ((mBuilder.isLoadCacheAnyway() && !noCache)) {
                    dispatchRequest = true;
                }

                deliverResult = !dispatchRequest;
            }

            if (mTag.error != 0 && !contentChanged) {
                dispatchRequest = true;
            }
        } else {
            dispatchRequest = true;
        }

        if (dispatchRequest) {
            Intent service = new Intent(mGetTagIntent);
            service.putExtra("local", mBuilder.localUri());
            service.putExtra("remote", mBuilder.remoteUri());
            service.putExtra("cache", mBuilder.cacheExpiry());
            service.putExtra("timeout", mBuilder.timeout());
            service.putExtra("params", mBuilder.params());
            service.putExtra("method", mBuilder.method());

            mContext.startService(service);
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
}