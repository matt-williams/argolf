package com.github.matt.williams.argolf;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TitleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        ((Button)findViewById(R.id.playButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TitleActivity.this, PlayersActivity.class));
            }
        });

        ((Button)findViewById(R.id.aboutButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TitleActivity.this, AboutActivity.class));
            }
        });
    }
}
