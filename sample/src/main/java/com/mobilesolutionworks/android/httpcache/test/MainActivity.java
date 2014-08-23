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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import com.mobilesolutionworks.android.httpcache.test.content.Example;
import com.mobilesolutionworks.android.httpcache.v4.HttpCacheLoader;

/**
 * Created by yunarta on 23/8/14.
 */
public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportLoaderManager().initLoader(0, null, new HttpCacheLoader(this, Example.create(Example.Resource.EXAMPLE1.uri + "?cache=30")) {
            @Override
            protected void nodata() {

            }

            @Override
            protected void use(int error, String data, long time) {
                Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void error(int error, String data) {

            }
        });
    }
}
