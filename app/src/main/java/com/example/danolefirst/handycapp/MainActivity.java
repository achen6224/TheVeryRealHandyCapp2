package com.example.danolefirst.handycapp;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        handleIntent(getIntent());


       /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }
    @Override
    protected void onNewIntent(Intent intent){
        handleIntent(intent);
    }

    DatabaseTable db = new DatabaseTable(this);
    private void handleIntent(Intent intent){
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String query = intent.getStringExtra(SearchManager.QUERY);
            Cursor c = db.getWordMatches(query, null);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        SearchManager searchManager =
                (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView)menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();
        FragmentManager fragmentManager = getFragmentManager();

        if (id == R.id.nav_first_layout) {
            fragmentManager.beginTransaction().replace(R.id.content_frame, new FirstFragment()).commit();
        } else if (id == R.id.nav_second_layout){
            fragmentManager.beginTransaction().replace(R.id.content_frame, new SecondFragment()).commit();
        }  else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public class DatabaseTable {
        public Cursor getWordMatches(String query, String[] columns) {
            String selection = COL_WORD + " Match ?";
            String[] selectionArgs = new String[] {query+"*"};

            return query(selection, selectionArgs, columns);
        }
        private Cursor query (String selection, String[] selectionArgs, String[] columns) {
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(FTS_VIRTUAL_TABLE);

            Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                    columns, selection, selectionArgs, null,null,null);

            if (cursor ==null) {
                return null;
            } else if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }
            return cursor;
        }


        private static final String TAG = "DictionaryDatabase";

        //The columns we'll include in the dictionary table
        public static final String COL_WORD = "WORD";
        public static final String COL_DEFINITION = "DEFINITION";

        private static final String DATABASE_NAME = "DICTIONARY";
        private static final String FTS_VIRTUAL_TABLE = "FTS";
        private static final int DATABASE_VERSION = 1;
        private final DatabaseOpenHelper mDatabaseOpenHelper;

        public DatabaseTable(Context context) {
            mDatabaseOpenHelper = new DatabaseOpenHelper(context);
        }
        private class DatabaseOpenHelper extends SQLiteOpenHelper {

            private final Context mHelperContext;
            private SQLiteDatabase mDatabase;

            private static final String FTS_TABLE_CREATE =
                    "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                            " USING fts3 (" +
                            COL_WORD + ", " +
                            COL_DEFINITION + ")";

            DatabaseOpenHelper(Context context) {
                super(context, DATABASE_NAME, null, DATABASE_VERSION);
                mHelperContext = context;
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                mDatabase = db;
                mDatabase.execSQL(FTS_TABLE_CREATE);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
                onCreate(db);
            }
            private void loadDictionary(){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            loadWords();
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }

                    }

                }).start();
            }
            private void loadWords()throws IOException {
                    final Resources resources = mHelperContext.getResources();
                    InputStream inputStream = resources.openRawResource(R.raw.definitions);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    try  {
                        String line;
                    while ((line = reader.readLine()) !=null) {
                        String[] strings = TextUtils.split(line,"-");
                        if (strings.length <2)continue;
                        long id = addWord(strings[0].trim(), strings[1].trim());
                    }

            } finally {
                reader.close();
            }
        }
        public long addWord(String word, String defintion) {
            ContentValues initalValues = new ContentValues();
            initalValues.put(COL_WORD, word);
            initalValues.put(COL_DEFINITION, definiton);

            return mDataBase.insert(FTS_VIRTUAL_TABLE,null, initalValues);

        }




    }
}
