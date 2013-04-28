package com.github.matt.williams.argolf;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.QueryOrder;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.TableQueryCallback;

public class DrivingRangeActivity extends ListActivity {
    public static final int REQUEST_CODE_SWING = 1;
    public static final String EXTRA_PLAYERS = "PLAYERS";

    public class Drive {
        public int id;
        public int Distance;
        public String Name;
        public int Timestamp;
    }

    private final ArrayList<Drive> mDrives = new ArrayList<Drive>();
    private ArrayAdapter<Drive> mAdapter;
    private String[] mPlayers;
    private int mPlayerIndex;
    private String mCurrentPlayer;
    private MobileServiceClient mClient;
    private Drive mLastDrive = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driving_range);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        mAdapter = new ArrayAdapter<Drive>(
                this,
                R.layout.list_item_driving_distance,
                mDrives) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        LayoutInflater inflator = DrivingRangeActivity.this.getLayoutInflater();
                        View rowView = inflator.inflate(R.layout.list_item_driving_distance, null, true);
                        Drive drive = mDrives.get(position);
                        ((TextView)rowView.findViewById(R.id.position)).setText((position + 1) + "");
                        ((TextView)rowView.findViewById(R.id.label)).setText(String.format("%1$.2fm - %2$s", drive.Distance / 100.0f, drive.Name));
                        ((TextView)rowView.findViewById(R.id.date)).setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(((long)drive.Timestamp) * 1000)));
                        if (drive == mLastDrive)
                        {
                            rowView.setBackgroundResource(android.R.color.darker_gray);
                        }
                        return rowView;
                    }
        };

        // Bind to our new adapter.
        setListAdapter(mAdapter);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

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

        try {
            mClient = new MobileServiceClient("https://argolf.azure-mobile.net/",
                                              Secrets.AZURE_API_KEY,
                                              this);
            mClient.getTable(Drive.class).orderBy("Distance", QueryOrder.Descending).top(100).execute(new TableQueryCallback<Drive>() {
                @Override
                public void onCompleted(List<Drive> drives, int count, Exception exception, ServiceFilterResponse response) {
                    if (exception != null) {
                        android.util.Log.e("DrivingRangeActivity", "MobileServiceClient threw exception", exception);
                    } else if (!drives.isEmpty()) {
                        mDrives.clear();
                        mDrives.addAll(drives);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        } catch (MalformedURLException e) {
            // TODO Handle me!
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == REQUEST_CODE_SWING) &&
            (resultCode == RESULT_OK)) {
            float speed = intent.getFloatExtra(SwingActivity.EXTRA_SPEED, 0.0f);
            float distance = 260.0f / 145.0f * 2.23f * speed;
            Drive drive = new Drive();
            drive.Name = mCurrentPlayer;
            drive.Distance = (int)(distance * 100);
            drive.Timestamp = (int)(new Date().getTime() / 1000);
            mClient.getTable(Drive.class).insert(drive, new TableOperationCallback<Drive>() {
                @Override
                public void onCompleted(Drive entity, Exception exception, ServiceFilterResponse response) {
                    if (exception == null) {
                        // Insert succeeded
                    } else {
                        android.util.Log.e("DrivingRangeActivity", "MobileServiceClient threw exception", exception);
                    }
                }
            });
            mDrives.add(drive);
            mLastDrive  = drive;
            Collections.sort(mDrives, new Comparator<Drive>() {
                @Override
                public int compare(Drive a, Drive b) {
                    return (a.Distance > b.Distance) ? -1 : (a.Distance == b.Distance) ? 0 : 1;
                }
            });
            mAdapter.notifyDataSetChanged();
            int driveIndex = mDrives.indexOf(drive);
            getListView().smoothScrollToPosition(driveIndex);
        }
        mPlayerIndex = (mPlayerIndex + 1) % mPlayers.length;
        mCurrentPlayer = mPlayers[mPlayerIndex];
    }
}
