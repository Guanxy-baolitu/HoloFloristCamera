package com.example.holofloristcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class WaitActivity extends AppCompatActivity {
    private Button reStartBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wait_layout);
        initView();
    }

    private void initView()
    {
        reStartBtn=findViewById(R.id.ReCaptureBtn);
        reStartBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(WaitActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        return;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
