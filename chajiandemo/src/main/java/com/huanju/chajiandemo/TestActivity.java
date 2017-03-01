package com.huanju.chajiandemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by DELL-PC on 2017/2/21.
 */

public class TestActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Main","test this = " + this);
        Log.e("Main","getResource = " + getResources());
        Button textView = new Button(this);
        textView.setText("我是插件Activity,我是代码布局，没有资源,再点我启动第二个");
        textView.setTextColor(Color.RED);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
        textView.setGravity(Gravity.CENTER);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TestActivity.this,TestActivityTwo.class));
            }
        });
        setContentView(textView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Main","插件onResume");
        Toast.makeText(getApplicationContext(),"我走了onResume方法",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Main","插件onDestroy");
    }
}
