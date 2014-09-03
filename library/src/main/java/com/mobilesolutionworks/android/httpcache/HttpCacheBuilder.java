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

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by yunarta on 24/8/14.
 */
public class HttpCacheBuilder implements Parcelable {

    private String mLocalUri;

    private String mRemoteUri;

    private int mCacheExpiry = 30;

    private int mTimeout;

    private boolean mLoadCacheAnyway = false;

    private boolean mNoCache;

    private Bundle mBundle;

    private String mMethod = "GET";

    public HttpCacheBuilder localUri(String localUri) {
        mLocalUri = localUri;
        return this;
    }

    public String localUri() {
        if (mLocalUri == null) {
            mLocalUri = md5(mRemoteUri);
            return mLocalUri;
        } else {
            return mLocalUri;
        }
    }

    public HttpCacheBuilder parseRemoteUri(String remoteUri) {
        Uri.Builder builder = Uri.parse(remoteUri).buildUpon();
        Uri uri = builder.build();

        Set<String> names = getQueryParameterNames(uri);
        if (!names.isEmpty()) {
            mBundle = new Bundle();
        }

        for (String name : names) {
            mBundle.putString(name, uri.getQueryParameter(name));
        }

        builder.query(null);
        mRemoteUri = builder.build().toString();

        return this;
    }

    public HttpCacheBuilder remoteUri(String remoteUri) {
        mRemoteUri = remoteUri;
        return this;
    }

    public String remoteUri() {
        return mRemoteUri;
    }

    public HttpCacheBuilder cacheExpiry(int cacheExpiry) {
        mCacheExpiry = cacheExpiry;
        return this;
    }

    public int cacheExpiry() {
        return mCacheExpiry;
    }

    public HttpCacheBuilder timeout(int timeout) {
        mTimeout = timeout;
        return this;
    }

    public int timeout() {
        return mTimeout;
    }

    public HttpCacheBuilder addParam(String name, String value) {
        if (mBundle == null) {
            mBundle = new Bundle();
        }

        mBundle.putString(name, value);
        return this;
    }

    public HttpCacheBuilder params(Bundle bundle) {
        if (mBundle == null) {
            mBundle = new Bundle();
        }

        mBundle.putAll(bundle);
        return this;
    }

    public Bundle params() {
        return mBundle;
    }

    public HttpCacheBuilder noCache() {
        mNoCache = true;
        return this;
    }

    public boolean isNoCache() {
        return mNoCache;
    }

    public HttpCacheBuilder loadCacheAnyway() {
        mLoadCacheAnyway = true;
        return this;
    }

    public boolean isLoadCacheAnyway() {
        return mLoadCacheAnyway;
    }


    public HttpCacheBuilder postMethod() {
        mMethod = "POST";
        return this;
    }

    public String method() {
        return mMethod;
    }

    private String md5(String uri) {
        StringBuilder sb = new StringBuilder();

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(uri.getBytes());

            if (mBundle != null) {
                Parcel obtain = Parcel.obtain();
                mBundle.writeToParcel(obtain, 0);
                md.update(obtain.marshall());
            }
            byte[] digest = md.digest();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }

        } catch (NoSuchAlgorithmException e) {
            // e.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @return a set of decoded names
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mLocalUri);
        dest.writeString(this.mRemoteUri);
        dest.writeInt(this.mCacheExpiry);
        dest.writeInt(this.mTimeout);
        dest.writeByte(mLoadCacheAnyway ? (byte) 1 : (byte) 0);
        dest.writeByte(mNoCache ? (byte) 1 : (byte) 0);
        dest.writeBundle(mBundle);
        dest.writeString(this.mMethod);
    }

    public HttpCacheBuilder() {
    }

    private HttpCacheBuilder(Parcel in) {
        this.mLocalUri = in.readString();
        this.mRemoteUri = in.readString();
        this.mCacheExpiry = in.readInt();
        this.mTimeout = in.readInt();
        this.mLoadCacheAnyway = in.readByte() != 0;
        this.mNoCache = in.readByte() != 0;
        mBundle = in.readBundle();
        this.mMethod = in.readString();
    }

    public static final Creator<HttpCacheBuilder> CREATOR = new Creator<HttpCacheBuilder>() {
        public HttpCacheBuilder createFromParcel(Parcel source) {
            return new HttpCacheBuilder(source);
        }

        public HttpCacheBuilder[] newArray(int size) {
            return new HttpCacheBuilder[size];
        }
    };
}
