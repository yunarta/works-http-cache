package com.mobilesolutionworks.android.httpcache.v4;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.Loader;

/**
 * Created by yunarta on 31/7/14.
 */
public class SyncCursorLoader extends Loader<Cursor>
{
    final ForceLoadContentObserver mObserver;

    Uri      mUri;
    String[] mProjection;
    String   mSelection;
    String[] mSelectionArgs;
    String   mSortOrder;

    Cursor mCursor;

    @Override
    protected void onForceLoad()
    {
        super.onForceLoad();

        Cursor cursor = getContext().getContentResolver().query(mUri, mProjection, mSelection, mSelectionArgs, mSortOrder);
        if (cursor != null)
        {
            // Ensure the cursor window is filled
            cursor.getCount();
            cursor.registerContentObserver(mObserver);
        }

        deliverResult(cursor);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor)
    {
        if (isReset())
        {
            // An async query came in while the loader is stopped
            if (cursor != null)
            {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted())
        {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed())
        {
            oldCursor.close();
        }
    }

    /**
     * Creates an empty unspecified CursorLoader.  You must follow this with
     * calls to {@link #setUri(android.net.Uri)}, {@link #setSelection(String)}, etc
     * to specify the query to perform.
     */
    public SyncCursorLoader(Context context)
    {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    /**
     * Creates a fully-specified CursorLoader.  See
     * {@link android.content.ContentResolver#query(android.net.Uri, String[], String, String[], String)
     * ContentResolver.query()} for documentation on the meaning of the
     * parameters.  These will be passed as-is to that call.
     */
    public SyncCursorLoader(Context context, Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder)
    {
        super(context);
        mObserver = new ForceLoadContentObserver();
        mUri = uri;
        mProjection = projection;
        mSelection = selection;
        mSelectionArgs = selectionArgs;
        mSortOrder = sortOrder;
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading()
    {
        if (mCursor != null)
        {
            deliverResult(mCursor);
        }

        if (takeContentChanged() || mCursor == null)
        {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading()
    {
        // Attempt to cancel the current load task if possible.
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed())
        {
            mCursor.close();
        }
        mCursor = null;
    }

    public Uri getUri()
    {
        return mUri;
    }

    public void setUri(Uri uri)
    {
        mUri = uri;
    }

    public String[] getProjection()
    {
        return mProjection;
    }

    public void setProjection(String[] projection)
    {
        mProjection = projection;
    }

    public String getSelection()
    {
        return mSelection;
    }

    public void setSelection(String selection)
    {
        mSelection = selection;
    }

    public String[] getSelectionArgs()
    {
        return mSelectionArgs;
    }

    public void setSelectionArgs(String[] selectionArgs)
    {
        mSelectionArgs = selectionArgs;
    }

    public String getSortOrder()
    {
        return mSortOrder;
    }

    public void setSortOrder(String sortOrder)
    {
        mSortOrder = sortOrder;
    }
}
