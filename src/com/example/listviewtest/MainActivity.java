package com.example.listviewtest;

import com.example.listviewtest.PullToReshListView.OnRefreshListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private String[] mItem = new String[40];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        for(int i=0; i<40; i++){
        	String s = "条目"+i;
        	mItem[i] = s;
        }
        
        final PullToReshListView listView = (PullToReshListView) findViewById(R.id.list); 
    	listView.setMode(PullToReshListView.MODE_BOTH);
    	MyAdapter adapter = new MyAdapter();
        listView.setAdapter(adapter);
    	
    	listView.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				Toast.makeText(getApplicationContext(), "刷新了", Toast.LENGTH_SHORT).show();
				listView.onRefreshComplete();
			}
			
			@Override
			public void onLoadMore() {
				Toast.makeText(getApplicationContext(), "加载更多了", Toast.LENGTH_SHORT).show();
				listView.onRefreshComplete();
			}
		});

    }
    
    private class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return mItem.length;
		}

		@Override
		public String getItem(int position) {
			return mItem[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView textView = new TextView(getApplicationContext());
			textView.setText(mItem[position]);
			return textView;
		}
    }

}
