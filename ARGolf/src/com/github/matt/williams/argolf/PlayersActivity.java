package com.github.matt.williams.argolf;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

public class PlayersActivity extends ListActivity {
    public static final String EXTRA_PLAYERS = "PLAYERS";
    public static final String SHARED_PREFERENCES_NAME = "ARGolf";
    public static final Pattern PREFERENCE_PATTERN = Pattern.compile("Player\\.([0-9]+)");;

    private final ArrayList<String> mPlayers = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;
    private SharedPreferences mSharedPreferences;
    private int mNumPlayers;

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

        ((Button)findViewById(R.id.playButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Editor editor = mSharedPreferences.edit();
                int playerIndex;
                for (playerIndex = 0; playerIndex < mPlayers.size(); playerIndex++) {
                    editor.putString("Player." + playerIndex, mPlayers.get(playerIndex));
                }
                for (; playerIndex < mNumPlayers; playerIndex++) {
                    editor.remove("Player." + playerIndex);
                }
                editor.commit();

                Intent intent = getIntent();
                intent.putExtra(EXTRA_PLAYERS, mPlayers.toArray(new String[]{}));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        mSharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        Map<String,?> preferences = mSharedPreferences.getAll();
        for (String key : preferences.keySet()) {
            Object value = preferences.get(key);
            Matcher matcher = PREFERENCE_PATTERN.matcher(key);
            if ((matcher.matches()) &&
                (value instanceof String)) {
                int playerIndex = Integer.valueOf(matcher.group(1));
                while (playerIndex >= mPlayers.size()) {
                    mPlayers.add(null);
                }
                mPlayers.set(playerIndex, (String)value);
            }
        }
        mNumPlayers = mPlayers.size();

        if (mPlayers.isEmpty()) {
            promptAddPlayer();
        }

        registerForContextMenu(getListView());
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_item_menu_player, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.delete:
            mPlayers.remove(info.position);
            mAdapter.notifyDataSetChanged();
            break;
        }
        return true;
    }
}
