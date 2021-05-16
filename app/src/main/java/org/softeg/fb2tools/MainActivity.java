package org.softeg.fb2tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements TransformModel.Observer,
        DirectoryChooserFragment.OnFragmentInteractionListener {

    private static final int PICKFILE_RESULT_CODE = App.getInstance().getUniqueIntValue();

    private static final String TAG = "MainActivity";
    private TransformModel mTransformModel;
    private static final String TAG_WORKER = "TAG_WORKER";

    private ArrayList<Map<String, String>> mData = new ArrayList<>();
    private DirectoryChooserFragment mDialog;
    private final String PATH_ATTRIBUTE = "PATH_ATTRIBUTE";
    private ListView mListView;
    private SimpleAdapter mAdapter;
    private View view_progress;
    private View add_dir_btn, add_file_btn, start_btn;
    private CheckBox checkBox;
    private TextView output_folder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        view_progress = findViewById(R.id.view_progress);

        output_folder = (TextView) findViewById(R.id.output_folder);
        output_folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDirClick(v);
            }
        });
        output_folder.setText(Html.fromHtml("<a href=\"\">" + AppPreferences.getOutputPath() + "</a>"));

        add_dir_btn = findViewById(R.id.add_dir_btn);
        add_file_btn = findViewById(R.id.add_file_btn);
        start_btn = findViewById(R.id.start_btn);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Очистить список")
                        .setMessage("Вы действительно хотите очистить список?")
                        .setPositiveButton("Очистить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mData.clear();
                                mAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .create().show();
            }
        });
        mAdapter = new SimpleAdapter(this, mData, android.R.layout.simple_list_item_1,
                new String[]{PATH_ATTRIBUTE},
                new int[]{android.R.id.text1});
        mListView.setAdapter(mAdapter);

        checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                start_btn.setEnabled(isChecked);
            }
        });

        mDialog = DirectoryChooserFragment.newInstance("DialogSample", null);
        final TransformWorkerFragment retainedWorkerFragment =
                (TransformWorkerFragment) getSupportFragmentManager().findFragmentByTag(TAG_WORKER);

        if (retainedWorkerFragment != null) {
            mTransformModel = retainedWorkerFragment.getTransformModel();
        } else {
            final TransformWorkerFragment workerFragment = new TransformWorkerFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(workerFragment, TAG_WORKER)
                    .commit();

            mTransformModel = workerFragment.getTransformModel();
        }

        mTransformModel.registerObserver(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("Data", getPathsStrings());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("Data")) {
            String[] pathsStrings = savedInstanceState.getStringArray("Data");
            mData.clear();
            for (String path : pathsStrings) {
                addPath(path);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

            String text = "Программа для вставки сносок сразу после ссылок на них<br/><br/>" +
                    "Copyright 2015 Artem Slinkin <a href=\"mailto:slartus@gmail.com\">slartus@gmail.com</a>";

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(App.getInstance().getProgramFullName())
                    .setMessage("Артём Слинкин")
                    .setMessage(Html.fromHtml(text))
                    .create();
            dialog.show();
            TextView textView = (TextView) dialog.findViewById(android.R.id.message);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void selectFileClick(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    public void selectDirClick(View view) {
        mDialog.show(getFragmentManager(), Integer.toString(view.getId()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == PICKFILE_RESULT_CODE) {
            String path = getRealPathFromURI(data.getData());
            if (!path.toLowerCase().endsWith(".fb2")) {
                Toast.makeText(this, "Можно выбрать только формат fb2", Toast.LENGTH_SHORT).show();
                return;
            }
            addPath(path);
            mAdapter.notifyDataSetChanged();
        }
//        else if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED && requestCode == REQUEST_DIRECTORY) {
//            mPaths.add(data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
//        }
    }

    private void addPath(String path) {
        Map<String, String> map = new HashMap<>();
        map.put(PATH_ATTRIBUTE, path);
        mData.add(map);
    }

    @Override
    public void onSelectDirectory(String path) {

        int viewId = Integer.parseInt(mDialog.getTag().toString());
        switch (viewId) {
            case R.id.output_folder:
                AppPreferences.trySetOutputPath(path);
                output_folder.setText(Html.fromHtml("<a href=\"\">" + AppPreferences.getOutputPath() + "</a>"));
                break;
            case R.id.add_dir_btn:
                addPath(path);
                mAdapter.notifyDataSetChanged();
                break;
        }


        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    private static String getRealPathFromURI(Uri contentUri) {
        if (!contentUri.toString().startsWith("content://"))
            return contentUri.getPath();

        // can post image
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = App.getInstance().getContentResolver().query(contentUri,
                    filePathColumn, // Which columns to return
                    null,       // WHERE clause; which rows to return (all rows)
                    null,       // WHERE clause selection arguments (none)
                    null); // Order-by clause (ascending by name)
            assert cursor != null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            return cursor.getString(column_index);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mTransformModel.unregisterObserver(this);

        if (isFinishing()) {
            mTransformModel.stopTransform();
        }
    }

    @Override
    public void onTransformStarted(TransformModel signInModel) {
        showProgress(true);
    }

    @Override
    public void onTransformCompleted(TransformModel signInModel) {
        showProgress(false);
        final HashMap<String, StringBuilder> booksWarnings =
                signInModel.getBooksWarnings();
        if (booksWarnings.size() > 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Завершено с сообщениями!")
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Показать сообщения", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StringBuilder sb = new StringBuilder();
                            for (String bookFileName : booksWarnings.keySet()) {
                                sb.append("<b>").append(bookFileName).append("</b><br/>")
                                        .append(booksWarnings.get(bookFileName)).append("</br>");
                            }
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Сообщения")
                                    .setMessage(Html.fromHtml(sb.toString()))
                                    .setPositiveButton("Закрыть", null)
                                    .create().show();
                        }
                    })
                    .create().show();
        } else
            new AlertDialog.Builder(this)
                    .setMessage("Завершено успешно!")
                    .setPositiveButton("OK", null)
                    .create().show();
    }

    @Override
    public void onTransformFailed(TransformModel signInModel, Throwable ex) {
        AppLog.e(this, ex);
        showProgress(false);
    }

    private void showProgress(final boolean show) {

        add_dir_btn.setEnabled(!show);
        add_file_btn.setEnabled(!show);

        start_btn.setEnabled(!show);

        checkBox.setEnabled(!show);
        mListView.setEnabled(!show);
        view_progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void transformClick(View view) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "Нет доступа к внешнему хранилищу!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkBox.isChecked())
            return;
        if (mData.size() == 0)
            return;
        String[] paths = getPathsStrings();
        mTransformModel.transform(paths);
    }

    private String[] getPathsStrings() {
        String[] paths = new String[mData.size()];
        int i = 0;
        for (Map<String, String> map : mData) {
            paths[i++] = map.get(PATH_ATTRIBUTE);
        }
        return paths;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
