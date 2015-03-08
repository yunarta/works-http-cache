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
public class HttpCacheRequest implements Parcelable {

    public static class Builder {

        HttpCacheRequest mRequest;

        public Builder()
        {
            mRequest = new HttpCacheRequest();
        }

        public Builder setKeepFresh() {
            mRequest.mKeepFresh = true;
            return this;
        }


        public Builder token(String token) {
            mRequest.mToken = token;
            return this;
        }

        public Builder addLocalUri(String addendum) {
            mRequest.mLocalUri += addendum;
            return this;
        }

        public Builder localUri(String localUri) {
            mRequest.mLocalUri = localUri;
            return this;
        }

        public Builder parseRemoteUri(String remoteUri) {
            Uri.Builder builder = Uri.parse(remoteUri).buildUpon();
            Uri uri = builder.build();

            Set<String> names = mRequest.getQueryParameterNames(uri);
            if (!names.isEmpty()) {
                mRequest.mBundle = new Bundle();
            }

            for (String name : names) {
                mRequest.mBundle.putString(name, uri.getQueryParameter(name));
            }

            builder.query(null);
            mRequest.mRemoteUri = builder.build().toString();

            return this;
        }

        public Builder remoteUri(String remoteUri) {
            mRequest.mRemoteUri = remoteUri;
            return this;
        }

        public Builder cacheExpiry(int cacheExpiry) {
            mRequest.mCacheExpiry = cacheExpiry;
            return this;
        }

        public Builder timeout(int timeout) {
            mRequest.mTimeout = timeout;
            return this;
        }

        public Builder addParam(String name, String value) {
            if (mRequest.mBundle == null) {
                mRequest.mBundle = new Bundle();
            }

            mRequest.mBundle.putString(name, value);
            return this;
        }

        public Builder params(Bundle bundle) {
            if (mRequest.mBundle == null) {
                mRequest.mBundle = new Bundle();
            }

            mRequest.mBundle.putAll(bundle);
            return this;
        }

        public Builder noCache() {
            mRequest.mNoCache = true;
            return this;
        }

        public Builder postMethod() {
            mRequest.mMethod = "POST";
            return this;
        }

        public Builder loadCacheAnyway() {
            mRequest.mLoadCacheAnyway = true;
            return this;
        }

        public HttpCacheRequest build() {
            return mRequest;
        }

        public Builder addParamsToLocalUri()
        {
            StringBuilder sb = new StringBuilder();

            Set<String> keys = mRequest.mBundle.keySet();
            for (String key : keys)
            {
                String value = mRequest.mBundle.getString(key);
                sb.append('&').append(Uri.encode(key)).append('=').append(Uri.encode(value));
            }

            if (sb.length() > 0)
            {
                sb.deleteCharAt(0);
                mRequest.mLocalUri += "?" + sb.toString();
            }

            return this;
        }
    }

    protected String mLocalUri;

    protected String mRemoteUri;

    protected int mCacheExpiry = 30;

    protected int mTimeout;

    protected boolean mLoadCacheAnyway = false;

    protected boolean mNoCache;

    protected Bundle mBundle;

    protected String mMethod = "GET";

    protected String mToken = "";

    protected boolean mKeepFresh = false;

    public boolean keepFresh() {
        return false;
    }

    public String token() {
        return mToken;
    }

    public String localUri() {
        if (mLocalUri == null) {
            mLocalUri = md5(mRemoteUri);
            return mLocalUri;
        } else {
            return mLocalUri;
        }
    }

    public String remoteUri() {
        return mRemoteUri;
    }

    public int cacheExpiry() {
        return mCacheExpiry;
    }

    public int timeout() {
        return mTimeout;
    }

    public Bundle params() {
        return mBundle;
    }

    public boolean isNoCache() {
        return mNoCache;
    }

    public boolean isLoadCacheAnyway() {
        return mLoadCacheAnyway;
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

    public HttpCacheRequest() {
    }

    private HttpCacheRequest(Parcel in) {
        this.mLocalUri = in.readString();
        this.mRemoteUri = in.readString();
        this.mCacheExpiry = in.readInt();
        this.mTimeout = in.readInt();
        this.mLoadCacheAnyway = in.readByte() != 0;
        this.mNoCache = in.readByte() != 0;
        mBundle = in.readBundle();
        this.mMethod = in.readString();
    }

    public static final Creator<HttpCacheRequest> CREATOR = new Creator<HttpCacheRequest>()
    {
        public HttpCacheRequest createFromParcel(Parcel source)
        {
            return new HttpCacheRequest(source);
        }

        public HttpCacheRequest[] newArray(int size)
        {
            return new HttpCacheRequest[size];
        }
    };
}
