package com.example.testexoplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListActivity extends android.app.ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] titles = new String[MyApp.getPlaySourceBeans().size()];
        for (int i = 0; i < MyApp.getPlaySourceBeans().size(); i++) {
            titles[i] = MyApp.getPlaySourceBeans().get(i).getChannelName();
        }
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));

        ViewGroup.LayoutParams layoutParams = getListView().getLayoutParams();
        layoutParams.width = 1000;
        getListView().setLayoutParams(layoutParams);
        getListView().setBackgroundColor(0xdd000000);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        setResult(Activity.RESULT_FIRST_USER,new Intent().putExtra(MainActivity.KEY_INDEX,position));
        finish();
    }
}
