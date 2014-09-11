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

import org.apache.http.NoHttpResponseException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yunarta on 8/6/14.
 */
public enum CacheErrorCode {

    UNKNOWN(0xffffffff, Category.OK),

    TIMEOUT_EXCEPTION(0x8000 | 0x15, Category.GENERIC_PROCESS_ERROR),
    IO_EXCEPTION(0x8000 | 0x14, Category.GENERIC_PROCESS_ERROR),
    SECURITY_EXCEPTION(0x8000 | 0x0a, Category.GENERIC_PROCESS_ERROR),
    GENERIC_PROCESS_ERROR(0x8000, Category.GENERIC_PROCESS_ERROR),

    NET_HTTP_NOT_FOUND(0x4000 | 0x194, Category.GENERIC_NET_ERROR),
    GENERIC_NET_ERROR(0x4000, Category.GENERIC_NET_ERROR),

    CANCELED(0x1, Category.OK),
    OK(0, Category.OK),
    DELETED(-1, Category.OK);

    private int mValue;

    private final Category mCategory;

    CacheErrorCode(int value, Category category) {
        mValue = value;
        mCategory = category;
    }

    public int value() {
        return mValue;
    }

    public boolean isProcessError() {
        return mCategory == Category.GENERIC_PROCESS_ERROR;
    }

    public boolean isNetError() {
        return mCategory == Category.GENERIC_NET_ERROR;
    }

    public boolean isOK() {
        return mCategory == Category.OK;
    }

    private static Map<Integer, CacheErrorCode> sCodeMap = new HashMap<Integer, CacheErrorCode>();

    static {
        CacheErrorCode[] values = CacheErrorCode.values();
        for (CacheErrorCode value : values) {
            sCodeMap.put(value.mValue, value);
        }
    }

    public static CacheErrorCode createNet(int value) {
        CacheErrorCode code = sCodeMap.get(0x4000 | value);
        return code == null ? UNKNOWN : code;
    }

    public static CacheErrorCode createException(Throwable throwable) {
        int value = 0;
        if (throwable instanceof SecurityException) {
            value = 10;
        } else if (throwable instanceof InterruptedIOException || throwable instanceof NoHttpResponseException) {
            value = 21;
        } else if (throwable instanceof IOException) {
            value = 20;
        }

        CacheErrorCode code = sCodeMap.get(0x8000 | value);
        return code == null ? GENERIC_PROCESS_ERROR : code;
    }

    public static CacheErrorCode get(int value) {
        CacheErrorCode code = sCodeMap.get(value);
        return code == null ? UNKNOWN : code;
    }

    public static CacheErrorCode getGeneric(int value) {
        int generic = value >> 12 << 12;
        return get(generic);
    }

    private enum Category {
        GENERIC_PROCESS_ERROR, GENERIC_NET_ERROR, OK
    }
}
