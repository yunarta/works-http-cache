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

package com.mobilesolutionworks.android.httpcache.test;

import android.net.Uri;
import com.mobilesolutionworks.android.httpcache.WorksHttpCacheService;

/**
 * Created by yunarta on 23/8/14.
 */
public class WorksHttpCacheServiceImpl extends WorksHttpCacheService {

    @Override
    protected String createUrl(Uri data, String[] args) {
        return "" + data.getEncodedPath();
    }

    @Override
    protected int resolveUri(Uri data) {
        return 0;
    }

    @Override
    protected Uri createUri(String path) {
        return null;
    }
}
