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

/**
 * Created by yunarta on 8/6/14.
 */
public class CacheErrorCode {
    public static final int UNKNOWN       = 0xffffffff;
    public static final int PROCESS_ERROR = 0x8000;
    public static final int NET_ERROR     = 0x4000;

    public static final int CANCELED = 1;
    public static final int OK       = 0;
    public static final int DELETED  = -1;

    public static int getGeneric(int code) {
        return code & 0xf000;
    }

    public static int createNet(int code) {
        return NET_ERROR | code;
    }
}
