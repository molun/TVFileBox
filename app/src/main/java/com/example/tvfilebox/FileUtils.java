package com.example.tvfilebox;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class FileUtils {
    private FileUtils() {}

    static File uploadDirectory(Context context) {
        File dir = context.getExternalFilesDir("uploads");
        if (dir == null) {
            dir = new File(context.getFilesDir(), "uploads");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    static List<File> listFiles(File directory) {
        List<File> result = new ArrayList<File>();
        File[] children = directory == null ? null : directory.listFiles();
        if (children != null) {
            Collections.addAll(result, children);
        }
        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                if (left.isDirectory() != right.isDirectory()) {
                    return left.isDirectory() ? -1 : 1;
                }
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
        return result;
    }

    static List<File> listUploads(Context context) {
        return listUploadsForDirectory(uploadDirectory(context));
    }

    static List<File> listUploadsForDirectory(File directory) {
        List<File> result = listFiles(directory);
        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                long delta = right.lastModified() - left.lastModified();
                if (delta == 0) return left.getName().compareToIgnoreCase(right.getName());
                return delta < 0 ? -1 : 1;
            }
        });
        return result;
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double value = bytes / 1024.0;
        if (value < 1024) return String.format(Locale.US, "%.1f KB", value);
        value /= 1024.0;
        if (value < 1024) return String.format(Locale.US, "%.1f MB", value);
        value /= 1024.0;
        return String.format(Locale.US, "%.2f GB", value);
    }

    static String mimeType(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        if (name.endsWith(".apk")) return "application/vnd.android.package-archive";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < name.length()) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1));
            if (type != null) return type;
        }
        return "application/octet-stream";
    }

    static void openFile(Context context, File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        if (file.isDirectory()) return;

        boolean apk = file.getName().toLowerCase(Locale.US).endsWith(".apk");
        if (apk && Build.VERSION.SDK_INT >= 26) {
            PackageManager pm = context.getPackageManager();
            if (!pm.canRequestPackageInstalls()) {
                try {
                    Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(permission);
                    Toast.makeText(context, "请允许本应用安装未知来源应用，然后再次打开 APK", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(context, "请在系统设置中允许安装未知来源应用", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }

        Uri uri = Build.VERSION.SDK_INT >= 24
                ? LocalFileProvider.uriForFile(context, file)
                : Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType(file));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "没有可打开此文件的应用", Toast.LENGTH_LONG).show();
        }
    }

    static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }
        return file.delete();
    }

    static File safeDestination(File directory, String requestedName) throws IOException {
        String clean = requestedName == null ? "upload.bin" : requestedName.replace('\\', '/');
        clean = new File(clean).getName().trim();
        if (clean.length() == 0 || ".".equals(clean) || "..".equals(clean)) clean = "upload.bin";

        File candidate = new File(directory, clean);
        String canonicalDir = directory.getCanonicalPath() + File.separator;
        if (!candidate.getCanonicalPath().startsWith(canonicalDir)) {
            throw new IOException("非法文件名");
        }
        if (!candidate.exists()) return candidate;

        int dot = clean.lastIndexOf('.');
        String base = dot > 0 ? clean.substring(0, dot) : clean;
        String extension = dot > 0 ? clean.substring(dot) : "";
        for (int i = 1; i < 10000; i++) {
            candidate = new File(directory, base + " (" + i + ")" + extension);
            if (!candidate.exists()) return candidate;
        }
        throw new IOException("同名文件过多");
    }

    static void copy(File source, File destination) throws IOException {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(destination);
        byte[] buffer = new byte[64 * 1024];
        try {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.getFD().sync();
        } finally {
            try { input.close(); } catch (IOException ignored) {}
            try { output.close(); } catch (IOException ignored) {}
        }
    }
}
