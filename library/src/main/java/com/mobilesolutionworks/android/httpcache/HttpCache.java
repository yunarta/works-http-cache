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

import android.database.Cursor;

/**
 * Created by yunarta on 24/8/14.
 */
public class HttpCache {

    public Cursor cursor;

    public String local;

    public String remote;

    public String content;

    public long expiry;

    public int error;

    public boolean loaded;

    public boolean detached;

    public Throwable trace;

    public int status;

    public boolean loadFinished = true;

    public void close() {
        if (!cursor.isClosed()) {
            cursor.close();
        }
    }
}
