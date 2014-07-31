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

/**
 * Created by yunarta on 31/7/14.
 */
public class WorksHttpCacheContentProvider extends ContentProvider
{
    protected SQLiteDatabase mDatabase;

    protected String mTableName;

    protected String mGetDataIntent;

    public WorksHttpCacheContentProvider(String tableName)
    {
        mTableName = tableName;
    }

    @Override
    public boolean onCreate()
    {
        Bundle metaData;
        try
        {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            metaData = new Bundle();
            metaData.putString("getDataIntent", "GET_DATA_INTENT");
        }

        mGetDataIntent = metaData.getString("getDataIntent");

        SQLiteOpenHelper SQLiteOpenHelper = new android.database.sqlite.SQLiteOpenHelper(getContext(), "cache", null, 4)
        {

            @Override
            public void onCreate(SQLiteDatabase db)
            {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + mTableName + " (" +
                        "uri TEXT," +
                        "json TEXT," +
                        "time INTEGER," +
                        "error INTEGER DEFAULT 0," +
                        "PRIMARY KEY (uri) ON CONFLICT REPLACE" +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
            {
                db.execSQL("DROP TABLE IF EXISTS " + mTableName);
                db.execSQL("CREATE TABLE IF NOT EXISTS " + mTableName + " (" +
                        "uri TEXT," +
                        "json TEXT," +
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
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder)
    {
        String timeLimit = String.valueOf(System.currentTimeMillis());
        String segment = uri.getEncodedPath();

        Cursor cursor = mDatabase.query(mTableName, columns, "uri = ? AND time > ? AND error != -1", new String[]{segment, timeLimit}, sortOrder, null, null);
        if (cursor != null && !cursor.moveToFirst())
        {
            Intent service = new Intent(mGetDataIntent);
            service.setData(uri);
            service.putExtra("params", selection);

            getContext().startService(service);
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri)
    {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        mDatabase.insert(mTableName, null, values);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        String segment = uri.getEncodedPath();
        if ("all".equals(segment))
        {
            return mDatabase.delete(mTableName, null, null);
        }
        else if (selectionArgs != null)
        {
            return mDatabase.delete(mTableName, "uri LIKE ?", selectionArgs);
        }
        else
        {
            return mDatabase.delete(mTableName, "uri = ?", new String[]{segment});
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        String segment = uri.getEncodedPath();
        return mDatabase.update(mTableName, values, "uri = ?", new String[]{segment});
    }
}