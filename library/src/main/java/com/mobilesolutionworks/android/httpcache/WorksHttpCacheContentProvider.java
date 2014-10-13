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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/**
 * Created by yunarta on 24/8/14.
 */
public class WorksHttpCacheContentProvider extends ContentProvider {

    protected static String TABLE_NAME = "cache";

    protected SQLiteDatabase mDatabase;

    @Override
    public boolean onCreate() {
        SQLiteOpenHelper SQLiteOpenHelper = new android.database.sqlite.SQLiteOpenHelper(getContext(), "tag-cache", null, 8) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        /* local uri    */ "local TEXT," +
                        /* remote uri   */ "remote TEXT," +
                        /* data content */ "data TEXT," +
                        /* expiry time  */ "time INTEGER," +
                        /* error code   */ "error INTEGER DEFAULT 0," +
                        "PRIMARY KEY (local) ON CONFLICT REPLACE" +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        };
        mDatabase = SQLiteOpenHelper.getWritableDatabase();

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDatabase.query(TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mDatabase.insert(TABLE_NAME, null, values);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return mDatabase.delete(TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return mDatabase.update(TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
