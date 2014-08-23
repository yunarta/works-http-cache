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

package com.mobilesolutionworks.android.httpcache.test.content;

import android.content.UriMatcher;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yunarta on 23/8/14.
 */
public class Example {

    private static final String AUTHORITY = "com.mobilesolutionworks.httpcache";

    public enum Resource {
        UNKNOWN("", ""),
        EXAMPLE1("/example1/", "https://raw.githubusercontent.com/yunarta/works-http-cache/master/sample/data/example1"),
        EXAMPLE2("/example2/", "https://raw.githubusercontent.com/yunarta/works-http-cache/master/sample/data/example2"),;

        public final String uri;

        public final String path;

        Resource(String uri, String path) {

            this.uri = uri;
            this.path = path;
        }

        public static Map<Integer, Resource> sEnums = new HashMap<Integer, Resource>();

        public static Resource resolve(Uri path) {
            int id = sMatcher.match(path);
            if (id == -1) return UNKNOWN;

            Resource resource = sEnums.get(id);
            return resource == null ? UNKNOWN : resource;
        }
    }

    private static final UriMatcher sMatcher;

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (Resource match : Resource.values()) {
            sMatcher.addURI(AUTHORITY, match.uri, match.ordinal());
            Resource.sEnums.put(match.ordinal(), match);
        }
    }

    public static Uri create(String path) {
        return Uri.parse("content://" + AUTHORITY + path);
    }
}
