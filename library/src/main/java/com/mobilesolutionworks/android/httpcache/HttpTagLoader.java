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
import android.content.Context;
import android.content.Loader;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class HttpTagLoader extends Loader<HttpTag> {

    HttpTagLoaderImpl mImplementation;

    final ForceLoadContentObserver mObserver;

    HttpTag mTag;

    public HttpTagLoader(Context context, HttpTagBuilder builder) {
        super(context);

        mImplementation = new HttpTagLoaderImpl(context, builder);
        mObserver = new ForceLoadContentObserver();
    }


    @Override
    protected void onForceLoad() {
        deliverResult(mImplementation.onForceLoad(mObserver));
    }

    @Override
    public void deliverResult(HttpTag tag) {
        if (isStarted() && mImplementation.deliverResult(tag)) {
            super.deliverResult(tag);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mTag != null) {
            deliverResult(mTag);
        }

        if (takeContentChanged() || mTag == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        mImplementation.onStopLoading();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        mImplementation.onReset();

        mTag = null;
    }
}