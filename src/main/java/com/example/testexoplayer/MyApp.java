package com.example.testexoplayer;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MyApp extends Application {
    private final String TAG = getClass().getSimpleName();
    private static List<PlaySourceBean> playSourceBeans;

    @Override
    public void onCreate() {
        super.onCreate();
//        loadPlaySources();
    }

    public static List<PlaySourceBean> getPlaySourceBeans(){
        return playSourceBeans;
    }

    public void loadPlaySources() {
        try {
            InputStream inputStream = getAssets().open("media.exolist.json");
            String str = getString(inputStream);
            playSourceBeans = new Gson().fromJson(str,new TypeToken<List<PlaySourceBean>>(){}.getType());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void setPlaySource(List<PlaySourceBean> list){
        playSourceBeans = list;
    }

    public static String getString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer("");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
