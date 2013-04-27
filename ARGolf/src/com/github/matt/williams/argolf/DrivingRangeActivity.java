package com.github.matt.williams.argolf;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;

public class DrivingRangeActivity extends ListActivity {
    public static final int REQUEST_CODE_SWING = 1;
    public static final String EXTRA_PLAYERS = "PLAYERS";

    private final ArrayList<String> mDrivingDistances = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;
    private String[] mPlayers;
    private int mPlayerIndex;
    private String mCurrentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driving_range);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        mAdapter = new ArrayAdapter<String>(
                this, // Context.
                R.layout.list_item_driving_distance,
                mDrivingDistances);

        // Bind to our new adapter.
        setListAdapter(mAdapter);

        ((Button)findViewById(R.id.driveButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DrivingRangeActivity.this, SwingActivity.class);
                intent.putExtra(SwingActivity.EXTRA_TITLE, mCurrentPlayer + "'s Swing");
                startActivityForResult(intent, REQUEST_CODE_SWING);
            }
        });

        mPlayers = getIntent().getStringArrayExtra(EXTRA_PLAYERS);
        mPlayerIndex = 0;
        mCurrentPlayer = mPlayers[mPlayerIndex];
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == REQUEST_CODE_SWING) &&
            (resultCode == RESULT_OK)) {
            float speed = intent.getFloatExtra(SwingActivity.EXTRA_SPEED, 0.0f);
            float distance = 260.0f / 145.0f * 2.23f * speed;
            mDrivingDistances.add(String.format("%1$.2fm - %2$s", distance, mCurrentPlayer));
            mAdapter.notifyDataSetChanged();
            getListView().smoothScrollToPosition(mDrivingDistances.size() - 1);
        }
        mPlayerIndex = (mPlayerIndex + 1) % mPlayers.length;
        mCurrentPlayer = mPlayers[mPlayerIndex];
    }
}
