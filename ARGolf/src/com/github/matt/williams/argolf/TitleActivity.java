package com.github.matt.williams.argolf;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TitleActivity extends Activity {

    protected static final int REQUEST_CODE_PLAY_ROUND = 1;
    protected static final int REQUEST_CODE_DRIVING_RANGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        ((Button)findViewById(R.id.roundButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(TitleActivity.this, PlayersActivity.class), REQUEST_CODE_PLAY_ROUND);
            }
        });

        ((Button)findViewById(R.id.drivingRangeButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(TitleActivity.this, PlayersActivity.class), REQUEST_CODE_DRIVING_RANGE);
            }
        });

        ((Button)findViewById(R.id.aboutButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TitleActivity.this, AboutActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            Intent newIntent;
            switch (requestCode) {
            case REQUEST_CODE_PLAY_ROUND:
                newIntent = new Intent(this, RoundActivity.class);
                newIntent.putExtra(DrivingRangeActivity.EXTRA_PLAYERS, intent.getStringArrayExtra(PlayersActivity.EXTRA_PLAYERS));
                startActivity(newIntent);
                break;
            case REQUEST_CODE_DRIVING_RANGE:
                newIntent = new Intent(this, DrivingRangeActivity.class);
                newIntent.putExtra(DrivingRangeActivity.EXTRA_PLAYERS, intent.getStringArrayExtra(PlayersActivity.EXTRA_PLAYERS));
                startActivity(newIntent);
                break;
            }
        }
    }
}
