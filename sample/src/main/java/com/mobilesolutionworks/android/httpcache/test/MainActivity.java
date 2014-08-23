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

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.mobilesolutionworks.android.httpcache.test.content.Example;
import com.mobilesolutionworks.android.httpcache.v4.HttpCacheLoader;

/**
 * Created by yunarta on 23/8/14.
 */
public class MainActivity extends FragmentActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText("");

        switch (v.getId()) {
            case R.id.btn1: {
                getSupportLoaderManager().destroyLoader(0);
                getSupportLoaderManager().initLoader(0, null, new HttpCacheLoader(this, Example.create(Example.Resource.EXAMPLE1.uri + "?cache=0")) {
                    @Override
                    protected void nodata() {

                    }

                    @Override
                    protected void use(int error, String data, long time) {
                        String text = "";
                        text += (time - System.currentTimeMillis()) + " ms before clearing";
                        text += "\n" + data;

                        TextView textView;

                        textView = (TextView) findViewById(R.id.from);
                        textView.setText("No Cache");

                        textView = (TextView) findViewById(R.id.text);
                        textView.setText(text);

                        // Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    protected void error(int error, String data) {

                    }
                });
                break;
            }

            case R.id.btn2: {
                getSupportLoaderManager().destroyLoader(0);
                getSupportLoaderManager().initLoader(0, null, new HttpCacheLoader(this, Example.create(Example.Resource.EXAMPLE1.uri + "?cache=30")) {
                    @Override
                    protected void nodata() {

                    }

                    @Override
                    protected void use(int error, String data, long time) {
                        String text = "";
                        text += (time - System.currentTimeMillis()) + " ms before clearing";
                        text += "\n" + data;

                        TextView textView;

                        textView = (TextView) findViewById(R.id.from);
                        textView.setText("With Cache");

                        textView = (TextView) findViewById(R.id.text);
                        textView.setText(text);
                    }

                    @Override
                    protected void error(int error, String data) {

                    }
                });
                break;
            }

            case R.id.btn3: {
                String uri = Example.Resource.EXAMPLE1.uri;
                Uri path = Example.create(uri);

                ContentResolver cr = getContentResolver();
                int delete = cr.delete(path, null, new String[]{uri + "%"});

                Toast.makeText(this, "Cache deleted", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

}
