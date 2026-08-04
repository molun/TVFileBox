package com.example.tvfilebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
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

        uploadDirectory = FileUtils.uploadDirectory(this);
        pathView.setText("文件保存完整路径：" + uploadDirectory.getAbsolutePath());
        adapter = new UploadEntryAdapter(this, FileUtils.listUploads(this), this);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startServer();
        refreshFiles(false);
    }

    @Override
    protected void onStop() {
        stopServer();
        super.onStop();
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
        if (!focusNewestAction || adapter.getCount() == 0) return;

        // Newly uploaded files are sorted first. Scroll to that row, wait for
        // ListView to lay it out, then focus its Open/Install button.
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
                        if (action != null && !action.requestFocus()) {
                            action.requestFocusFromTouch();
                        }
                    }
                });
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

}
