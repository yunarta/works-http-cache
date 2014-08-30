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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by yunarta on 31/7/14.
 */
public class WorksHttpCacheContentProvider extends ContentProvider {

    protected static String TABLE_NAME = "cache";

    protected SQLiteDatabase mDatabase;

    protected String mGetDataIntent;

    @Override
    public boolean onCreate() {
        Bundle metaData;
        try {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            metaData = new Bundle();
            metaData.putString("getDataIntent", "GET_DATA_INTENT");
        }

        mGetDataIntent = metaData.getString("getDataIntent");

        SQLiteOpenHelper SQLiteOpenHelper = new android.database.sqlite.SQLiteOpenHelper(getContext(), "cache", null, 6) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        "uri TEXT," +
                        "data TEXT," +
                        "time INTEGER," +
                        "error INTEGER DEFAULT 0," +
                        "PRIMARY KEY (uri) ON CONFLICT REPLACE" +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        "uri TEXT," +
                        "data TEXT," +
                        "time INTEGER," +
                        "error INTEGER DEFAULT 0," +
                        "PRIMARY KEY (uri) ON CONFLICT REPLACE" +
                        ")");
            }
        };
        mDatabase = SQLiteOpenHelper.getWritableDatabase();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        Log.d("WorksHttpCache", "query " + uri);

        String timeLimit = String.valueOf(System.currentTimeMillis());
        // String segment = uri.getEncodedPath();

        Set<String> names = getQueryParameterNames(uri);
        Uri.Builder builder = uri.buildUpon();
        builder.query(null);

        String error = "error != -1";
        for (String name : names) {
            if ("cache".equals(name) || "timeout".equals(name) || "test".equals(name)) {
                if ("test".equals(name)) {
                    error = "error = 0";
                }
                continue;
            }

            builder.appendQueryParameter(name, uri.getQueryParameter(name));
        }

        Uri build = builder.build();
        String path = build.getEncodedPath();
        if (build.getEncodedQuery() != null) {
            path += "?" + build.getEncodedQuery();
        }

        Cursor cursor = mDatabase.query(TABLE_NAME, columns, "uri = ? AND time > ? AND " + error, new String[]{path, timeLimit}, sortOrder, null, null);
        if (cursor != null && !cursor.moveToFirst()) {


            Intent service = new Intent(mGetDataIntent);
            service.setData(uri);
            service.putExtra("params", selection);
            service.putExtra("args", selectionArgs);

            getContext().startService(service);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mDatabase.insert(TABLE_NAME, null, values);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String segment = uri.getEncodedPath();
        if ("all".equals(segment)) {
            return mDatabase.delete(TABLE_NAME, null, null);
        } else if (selectionArgs != null) {
            return mDatabase.delete(TABLE_NAME, "uri LIKE ?", selectionArgs);
        } else {
            return mDatabase.delete(TABLE_NAME, "uri = ?", new String[]{segment});
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Set<String> names = getQueryParameterNames(uri);
        Uri.Builder builder = uri.buildUpon();
        builder.query(null);

        for (String name : names) {
            if ("cache".equals(name) || "timeout".equals(name) || "test".equals(name)) {
                continue;
            }

            builder.appendQueryParameter(name, uri.getQueryParameter(name));
        }

        Uri build = builder.build();
        String path = build.getEncodedPath();
        if (build.getEncodedQuery() != null) {
            path += "?" + build.getEncodedQuery();
        }

        if (values != null) {
            return mDatabase.update(TABLE_NAME, values, "uri = ?", new String[]{path});
        } else {
            values = new ContentValues();
            values.put("error", CacheErrorCode.DELETED.value());

            return mDatabase.update(TABLE_NAME, values, "uri = ? AND error >= 32768", new String[]{path});

        }
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @return a set of decoded names
     */
    public Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(uri.decode(name));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }
}