package com.example.tvfilebox;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    private static final int STORAGE_PERMISSION_REQUEST = 100;

    private TextView pathView;
    private ListView listView;
    private FileEntryAdapter adapter;
    private File initialDirectory;
    private File currentDirectory;
    private boolean confirmLongPressHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pathView = (TextView) findViewById(R.id.text_path);
        listView = (ListView) findViewById(R.id.file_list);
        initialDirectory = Environment.getExternalStorageDirectory();
        currentDirectory = initialDirectory;
        adapter = new FileEntryAdapter(this, FileUtils.listFiles(currentDirectory));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openEntry(adapter.getItem(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmLongPressHandled = true;
                deleteEntry(adapter.getItem(position));
                return true;
            }
        });
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private View previous;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (previous != null && previous != view) previous.setSelected(false);
                view.setSelected(true);
                previous = view;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (previous != null) previous.setSelected(false);
                previous = null;
            }
        });
        listView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && listView.getSelectedItemPosition() == AdapterView.INVALID_POSITION
                        && adapter.getCount() > 0) {
                    listView.setSelection(0);
                }
            }
        });

        ((Button) findViewById(R.id.button_home)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openDirectory(initialDirectory); }
        });
        ((Button) findViewById(R.id.button_new_folder)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCreateFolderDialog(); }
        });
        ((Button) findViewById(R.id.button_transfer)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openTransfer(); }
        });

        requestStoragePermissionIfNeeded();
        refresh();
        findViewById(R.id.button_home).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, STORAGE_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            refresh();
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未授予存储权限，只能访问应用自己的上传目录", Toast.LENGTH_LONG).show();
                openDirectory(FileUtils.uploadDirectory(this));
            }
        }
    }

    private void refresh() {
        if (currentDirectory == null || !currentDirectory.exists() || !currentDirectory.canRead()) {
            currentDirectory = FileUtils.uploadDirectory(this);
        }
        pathView.setText("当前路径：" + currentDirectory.getAbsolutePath());
        adapter.replace(FileUtils.listFiles(currentDirectory));
    }

    private void openDirectory(File directory) {
        if (directory != null && directory.isDirectory() && directory.canRead()) {
            currentDirectory = directory;
            refresh();
            listView.setSelection(0);
        } else {
            Toast.makeText(this, "无法访问此目录", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateParent() {
        File parent = currentDirectory == null ? null : currentDirectory.getParentFile();
        if (parent != null && parent.canRead()) openDirectory(parent);
        else Toast.makeText(this, "已经到达最上级目录", Toast.LENGTH_SHORT).show();
    }

    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("文件夹名称");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新建文件夹")
                .setView(input)
                .setPositiveButton("创建", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (!isSimpleName(name)) {
                            Toast.makeText(MainActivity.this, "名称不能为空，也不能包含路径分隔符", Toast.LENGTH_LONG).show();
                            return;
                        }
                        File folder = new File(currentDirectory, name);
                        if (folder.exists()) {
                            Toast.makeText(MainActivity.this, "同名文件或文件夹已经存在", Toast.LENGTH_SHORT).show();
                        } else if (!folder.mkdir()) {
                            Toast.makeText(MainActivity.this, "创建失败，目录可能不可写", Toast.LENGTH_LONG).show();
                        }
                        refresh();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialog) {
                input.requestFocus();
                dialogKeyboard(input);
            }
        });
        dialog.show();
    }

    private void openEntry(File file) {
        if (file.isDirectory()) openDirectory(file);
        else FileUtils.openFile(this, file);
    }

    private void renameEntry(final File file) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(file.getName());
        input.setSelection(input.getText().length());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("重命名")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (!isSimpleName(name)) {
                            Toast.makeText(MainActivity.this, "名称无效", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File destination = new File(file.getParentFile(), name);
                        if (destination.exists()) {
                            Toast.makeText(MainActivity.this, "同名文件已经存在", Toast.LENGTH_SHORT).show();
                        } else if (!file.renameTo(destination)) {
                            Toast.makeText(MainActivity.this, "重命名失败", Toast.LENGTH_SHORT).show();
                        }
                        refresh();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialog) { input.requestFocus(); dialogKeyboard(input); }
        });
        dialog.show();
    }

    private void deleteEntry(final File file) {
        new AlertDialog.Builder(this)
                .setTitle("永久删除")
                .setMessage("确定永久删除“" + file.getName() + "”吗？此操作无法撤销。")
                .setPositiveButton("永久删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (!FileUtils.deleteRecursively(file)) {
                            Toast.makeText(MainActivity.this, "删除失败，文件可能正在使用或目录不可写", Toast.LENGTH_LONG).show();
                        }
                        refresh();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isSimpleName(String name) {
        return name.length() > 0 && !".".equals(name) && !"..".equals(name)
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0;
    }

    private void dialogKeyboard(EditText input) {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void openTransfer() {
        startActivity(new Intent(this, TransferActivity.class));
    }

    private void showSelectedEntryActions() {
        int position = listView.getSelectedItemPosition();
        if (position == AdapterView.INVALID_POSITION || position >= adapter.getCount()) {
            Toast.makeText(this, "请先在文件列表中选择一项", Toast.LENGTH_SHORT).show();
            listView.requestFocus();
            if (adapter.getCount() > 0) listView.setSelection(0);
            return;
        }
        final File selected = adapter.getItem(position);
        new AlertDialog.Builder(this)
                .setTitle(selected.getName())
                .setItems(new CharSequence[] { "重命名", "删除" }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) renameEntry(selected);
                        else deleteEntry(selected);
                    }
                })
                .show();
    }

    private boolean deleteSelectedEntryFromRemote() {
        int position = listView.getSelectedItemPosition();
        if (!listView.hasFocus() || position == AdapterView.INVALID_POSITION || position >= adapter.getCount()) {
            return false;
        }
        deleteEntry(adapter.getItem(position));
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean contextMenuKey = RemoteKeyPolicy.isContextMenuKey(keyCode);
        if (contextMenuKey) {
            if (RemoteKeyPolicy.shouldOpenEntryActions(event.getAction(), keyCode, event.getRepeatCount())) {
                showSelectedEntryActions();
            }
            // 同时消费 DOWN 和 UP，防止系统在松键时再次处理该按键。
            return true;
        }
        if (RemoteKeyPolicy.isConfirmKey(keyCode)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    confirmLongPressHandled = false;
                } else if (RemoteKeyPolicy.shouldHandleConfirmLongPress(
                        event.getAction(), keyCode, event.getRepeatCount())) {
                    if (!confirmLongPressHandled) {
                        confirmLongPressHandled = deleteSelectedEntryFromRemote();
                    }
                    if (confirmLongPressHandled) return true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && confirmLongPressHandled) {
                confirmLongPressHandled = false;
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        File external = Environment.getExternalStorageDirectory();
        if (currentDirectory != null && external != null &&
                !currentDirectory.getAbsolutePath().equals(external.getAbsolutePath()) &&
                currentDirectory.getParentFile() != null && currentDirectory.getParentFile().canRead()) {
            navigateParent();
        } else {
            super.onBackPressed();
        }
    }
}
