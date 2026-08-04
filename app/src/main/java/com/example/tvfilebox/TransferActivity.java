package com.example.tvfilebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

public class TransferActivity extends Activity implements UploadEntryAdapter.Listener, EmbeddedHttpServer.Listener {
    private TextView urlView;
    private TextView pathView;
    private TextView statusView;
    private ImageView qrView;
    private ListView listView;
    private UploadEntryAdapter adapter;
    private EmbeddedHttpServer server;
    private String token;
    private File uploadDirectory;
    private int consumedNavigationKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private boolean focusFirstActionWhenWindowIsReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_transfer);

        urlView = (TextView) findViewById(R.id.text_server_url);
        pathView = (TextView) findViewById(R.id.text_upload_path);
        statusView = (TextView) findViewById(R.id.text_server_status);
        qrView = (ImageView) findViewById(R.id.image_qr);
        listView = (ListView) findViewById(R.id.upload_list);
        // Some Android TV 4.x ListView implementations keep focus on the
        // ListView itself unless focusable adapter descendants are enabled.
        listView.setItemsCanFocus(true);
        listView.setFocusable(false);

        uploadDirectory = FileUtils.uploadDirectory(this);
        pathView.setText("文件保存完整路径：" + uploadDirectory.getAbsolutePath());
        adapter = new UploadEntryAdapter(this, FileUtils.listUploads(this), this);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startServer();
        // Focus the first file's Open/Install action after the list is laid out.
        // The button stays non-focusable in touch mode so a tap activates it
        // immediately instead of only moving focus on the first tap.
        refreshFiles(true);
    }

    @Override
    protected void onStop() {
        stopServer();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && focusFirstActionWhenWindowIsReady) {
            focusFirstUploadAction();
        }
    }

    private void startServer() {
        stopServer();
        token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int selectedPort = -1;
        for (int port = 8080; port <= 8090; port++) {
            EmbeddedHttpServer candidate = new EmbeddedHttpServer(port, uploadDirectory, token, this);
            try {
                candidate.start(5000, false);
                server = candidate;
                selectedPort = port;
                break;
            } catch (IOException ignored) {
                candidate.stop();
            }
        }

        String ip = findLocalIpv4();
        if (server == null) {
            urlView.setText("HTTP 服务启动失败");
            statusView.setText("8080–8090 端口均不可用");
            qrView.setImageDrawable(null);
            return;
        }
        if (ip == null) {
            urlView.setText("请先连接 Wi-Fi");
            statusView.setText("服务已启动，但没有找到局域网 IPv4 地址");
            qrView.setImageDrawable(null);
            return;
        }

        String url = "http://" + ip + ":" + selectedPort + "/?token=" + token;
        urlView.setText(url);
        statusView.setText("服务运行中 · 仅本页打开期间可访问 · 手机和电视须在同一 Wi-Fi");
        try {
            Bitmap qr = QrCodeUtil.create(url, 420);
            qrView.setImageBitmap(qr);
        } catch (WriterException e) {
            statusView.setText("服务运行中，但二维码生成失败：" + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private String findLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;
            for (NetworkInterface network : Collections.list(interfaces)) {
                if (!network.isUp() || network.isLoopback()) continue;
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void refreshFiles(boolean focusNewestAction) {
        adapter.replace(FileUtils.listUploads(this));
        focusFirstActionWhenWindowIsReady = focusNewestAction && adapter.getCount() > 0;
        if (!focusFirstActionWhenWindowIsReady) return;

        focusFirstUploadAction();
    }

    private void focusFirstUploadAction() {
        if (adapter.getCount() == 0) {
            focusFirstActionWhenWindowIsReady = false;
            return;
        }

        // Newly uploaded files are sorted first. Scroll to that row, wait for
        // ListView and the Activity window to be ready, then focus its
        // Open/Install button.
        listView.setSelectionFromTop(0, 0);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelectionFromTop(0, 0);
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        View firstRow = listView.getChildAt(0);
                        if (firstRow == null) return;
                        View action = firstRow.findViewById(R.id.upload_open);
                        if (action != null && action.requestFocus()) {
                            focusFirstActionWhenWindowIsReady = false;
                        }
                    }
                });
            }
        });
    }

    private void focusUploadAction(int adapterPosition, final int actionViewId) {
        int childIndex = adapterPosition - listView.getFirstVisiblePosition();
        View row = childIndex >= 0 ? listView.getChildAt(childIndex) : null;
        if (row != null) {
            View action = row.findViewById(actionViewId);
            if (action != null) {
                if (!action.requestFocus()) action.requestFocusFromTouch();
                if (action.hasFocus()) return;
            }
        }

        listView.setSelection(adapterPosition);
        listView.post(new Runnable() {
            @Override
            public void run() {
                int selected = listView.getSelectedItemPosition();
                int index = selected - listView.getFirstVisiblePosition();
                View selectedRow = index >= 0 ? listView.getChildAt(index) : null;
                if (selectedRow == null) return;
                View action = selectedRow.findViewById(actionViewId);
                if (action != null && !action.requestFocus()) action.requestFocusFromTouch();
            }
        });
    }

    @Override
    public void onOpen(final File file) {
        FileUtils.openFile(this, file);
    }

    @Override
    public void onDelete(final File file) {
        new AlertDialog.Builder(this)
                .setTitle("永久删除")
                .setMessage("确定永久删除“" + file.getName() + "”吗？此操作无法撤销。")
                .setPositiveButton("永久删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (!FileUtils.deleteRecursively(file)) {
                            Toast.makeText(TransferActivity.this, "删除失败", Toast.LENGTH_LONG).show();
                        }
                        refreshFiles(false);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onFilesChanged() {
        runOnUiThread(new Runnable() {
            @Override public void run() { refreshFiles(true); }
        });
    }

    @Override
    public void onOpenRequested(final File file) {
        runOnUiThread(new Runnable() {
            @Override public void run() { FileUtils.openFile(TransferActivity.this, file); }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_UP && consumedNavigationKeyCode == keyCode) {
            consumedNavigationKeyCode = KeyEvent.KEYCODE_UNKNOWN;
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && isDpadDirection(keyCode)) {
            View focused = getCurrentFocus();
            if (adapter.getCount() > 0 && (focused == null ||
                    (focused.getId() != R.id.upload_open && focused.getId() != R.id.upload_delete))) {
                // A few TV firmwares remain in touch mode after launching an
                // Activity. The first DPAD event exits that mode; explicitly
                // establish button focus instead of leaving an invisible
                // ListView selection.
                consumedNavigationKeyCode = keyCode;
                focusUploadAction(0, R.id.upload_open);
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
            View focused = getCurrentFocus();
            if (focused != null &&
                    (focused.getId() == R.id.upload_open || focused.getId() == R.id.upload_delete)) {
                int current = listView.getPositionForView(focused);
                int target = RemoteKeyPolicy.nextUploadPosition(current, keyCode, adapter.getCount());
                if (target >= 0) {
                    consumedNavigationKeyCode = keyCode;
                    focusUploadAction(target, focused.getId());
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private static boolean isDpadDirection(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

}
