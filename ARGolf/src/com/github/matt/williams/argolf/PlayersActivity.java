package com.github.matt.williams.argolf;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

public class PlayersActivity extends ListActivity {
    ArrayList<String> mPlayers = new ArrayList<String>();
    ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_players);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        mAdapter = new ArrayAdapter<String>(
                this, // Context.
                R.layout.list_item_player,
                mPlayers);

        // Bind to our new adapter.
        setListAdapter(mAdapter);

        ((Button)findViewById(R.id.addButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                promptAddPlayer();
            }
        });

        promptAddPlayer();
    }

    private void promptAddPlayer() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getResources().getString(R.string.add_player));
        alert.setMessage(String.format(getResources().getString(R.string.enter_player_name), mPlayers.size() + 1));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mPlayers.add(input.getText().toString());
                mAdapter.notifyDataSetChanged();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mPlayers.isEmpty()) {
                    PlayersActivity.this.finish();
                }
            }
        });

        alert.show();
    }
}
