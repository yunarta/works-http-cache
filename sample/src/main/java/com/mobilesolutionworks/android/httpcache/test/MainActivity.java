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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.mobilesolutionworks.android.httpcache.HttpCacheBuilder;
import com.mobilesolutionworks.android.httpcache.v4.HttpCacheLoaderManager;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onClick(View v) {
//        TextView textView = (TextView) findViewById(R.id.text);
//        textView.setText("");

        switch (v.getId()) {
            case R.id.btn1: {
                HttpCacheBuilder builder = new HttpCacheBuilder().
                        parseRemoteUri("http://blog.yunarta.com/test/?a=b").
                        addParam("name", "yunarta").
                        noCache();

                getSupportLoaderManager().destroyLoader(0);
                getSupportLoaderManager().initLoader(0, null, new HttpCacheLoaderManager(this, builder) {

                    @Override
                    protected void nodata() {
                        Log.d(BuildConfig.DEBUG_TAG, "MainActivity.nodata");
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
                    }

                    @Override
                    protected void error(int error, String data) {
                        Log.d(BuildConfig.DEBUG_TAG, "error = " + error);
                    }
                });
                break;
            }

            case R.id.btn2: {
                HttpCacheBuilder builder = new HttpCacheBuilder().
                        remoteUri("http://blog.yunarta.com/test/").
                        addParam("name", "john doe").
                        cacheExpiry(10);

                getSupportLoaderManager().destroyLoader(0);
                getSupportLoaderManager().initLoader(0, null, new HttpCacheLoaderManager(this, builder) {

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
                    }

                    @Override
                    protected void error(int error, String data) {

                    }
                });
                break;
            }

            case R.id.btn3: {
                HttpCacheBuilder builder = new HttpCacheBuilder().
                        remoteUri("http://blog.yunarta.com/test-failed/").
                        addParam("name", "john doe").
                        cacheExpiry(10);

                getSupportLoaderManager().destroyLoader(0);
                getSupportLoaderManager().initLoader(0, null, new HttpCacheLoaderManager(this, builder) {

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
                        textView.setText("Error Test");

                        textView = (TextView) findViewById(R.id.text);
                        textView.setText(text);
                    }

                    @Override
                    protected boolean pf(int error, Throwable trace, String data) {
                        StringWriter wr = new StringWriter();

                        trace.printStackTrace(new PrintWriter(wr));
                        TextView textView;

                        textView = (TextView) findViewById(R.id.from);
                        textView.setText("Error Test");

                        textView = (TextView) findViewById(R.id.text);
                        textView.setText("error = " + error + ", data = " + data + ", trace = " + trace + "\n" + wr.toString());

                        return super.pf(error, trace, data);
                    }

                    @Override
                    protected void error(int error, String data) {
//                        TextView textView;
//
//                        textView = (TextView) findViewById(R.id.from);
//                        textView.setText("Error Test");
//
//                        textView = (TextView) findViewById(R.id.text);
//                        textView.setText("error = " + error + ", data = " + data);
                    }
                });
                break;
            }

            case R.id.btn4: {
                Toast.makeText(this, "Function not implemented", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }
}
