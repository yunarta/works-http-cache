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
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

/**
 * Created by yunarta on 13/9/14.
 */
public class HttpCacheUtil {

    public static void save(Context context, String localUri, String remoteUri, String data, String expiry, int errorCode) {
        HttpCacheConfiguration configuration = HttpCacheConfiguration.configure(context);

        ContentValues values = new ContentValues();
        values.put("local", localUri);
        values.put("remote", remoteUri);
        values.put("data", data);
        values.put("time", expiry);
        values.put("error", errorCode);

        Uri uri = Uri.withAppendedPath(configuration.authority, localUri);

        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.insert(uri, values);
        contentResolver.notifyChange(uri, null);
    }

    public static void deleteAll(Context context) {
        HttpCacheConfiguration configuration = HttpCacheConfiguration.configure(context);

        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(configuration.authority, null, null);
    }
}
