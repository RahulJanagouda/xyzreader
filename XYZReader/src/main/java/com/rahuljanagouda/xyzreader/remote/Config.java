package com.rahuljanagouda.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * credits: https://github.com/DmitryMalkovich/make-your-app-material
 */

public class Config {
    public static final URL BASE_URL;
    public static String LOG_TAG = Config.class.getSimpleName();

    static {
        URL url = null;
        try {
            url = new URL("https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json");
        } catch (MalformedURLException ignored) {
            Log.w(LOG_TAG, ignored.getMessage(), ignored);
        }

        BASE_URL = url;
    }
}
