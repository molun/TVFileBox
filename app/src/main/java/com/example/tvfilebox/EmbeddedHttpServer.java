package com.example.tvfilebox;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

final class EmbeddedHttpServer extends NanoHTTPD {
    interface Listener {
        void onFilesChanged();
        void onOpenRequested(File file);
    }

    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L * 1024L;

    private final File uploadDirectory;
    private final String token;
    private final Listener listener;

    EmbeddedHttpServer(int port, File uploadDirectory, String token, Listener listener) {
        super(port);
        this.uploadDirectory = uploadDirectory;
        this.token = token;
        this.listener = listener;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();
        if (!token.equals(params.get("token"))) {
            return text(Response.Status.FORBIDDEN, "访问链接无效或已经过期");
        }

        String uri = session.getUri();
        try {
            if (Method.POST.equals(session.getMethod()) && "/upload".equals(uri)) {
                return handleUpload(session);
            }
            if (Method.POST.equals(session.getMethod()) && "/action".equals(uri)) {
                return handleAction(session);
            }
            if (Method.GET.equals(session.getMethod()) && "/".equals(uri)) {
                return html(renderPage());
            }
            return text(Response.Status.NOT_FOUND, "Not found");
        } catch (Exception e) {
            return text(Response.Status.INTERNAL_ERROR, "操作失败：" + e.getMessage());
        }
    }

    private Response handleUpload(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> tempFiles = new java.util.HashMap<String, String>();
        session.parseBody(tempFiles);
        String tempPath = tempFiles.get("file");
        if (tempPath == null && !tempFiles.isEmpty()) tempPath = tempFiles.values().iterator().next();
        if (tempPath == null) return text(Response.Status.BAD_REQUEST, "没有收到文件");

        File source = new File(tempPath);
        if (source.length() > MAX_UPLOAD_BYTES) {
            return text(Response.Status.BAD_REQUEST, "文件超过 2 GB 限制");
        }
        if (uploadDirectory.getUsableSpace() < source.length() + 1024L * 1024L) {
            return text(Response.Status.INTERNAL_ERROR, "电视存储空间不足");
        }

        String requestedName = session.getParms().get("filename");
        if (requestedName == null) requestedName = session.getParms().get("file");
        File destination = FileUtils.safeDestination(uploadDirectory, requestedName);
        File partial = new File(uploadDirectory, "." + destination.getName() + ".part");
        if (partial.exists()) partial.delete();
        try {
            FileUtils.copy(source, partial);
            if (!partial.renameTo(destination)) {
                FileUtils.copy(partial, destination);
                partial.delete();
            }
        } catch (IOException e) {
            partial.delete();
            destination.delete();
            throw e;
        }
        listener.onFilesChanged();
        return text(Response.Status.OK, "上传成功：" + destination.getName());
    }

    private Response handleAction(IHTTPSession session) throws IOException, ResponseException {
        session.parseBody(new java.util.HashMap<String, String>());
        String action = session.getParms().get("action");
        String requestedName = session.getParms().get("name");
        File file = findExisting(requestedName);
        if (file == null) return text(Response.Status.NOT_FOUND, "文件不存在");

        if ("open".equals(action)) {
            listener.onOpenRequested(file);
            return text(Response.Status.OK, "已在电视上打开");
        }
        if ("delete".equals(action)) {
            if (!FileUtils.deleteRecursively(file)) {
                return text(Response.Status.INTERNAL_ERROR, "删除失败");
            }
            listener.onFilesChanged();
            return text(Response.Status.OK, "已永久删除");
        }
        return text(Response.Status.BAD_REQUEST, "未知操作");
    }

    private File findExisting(String requestedName) throws IOException {
        if (requestedName == null) return null;
        String clean = requestedName.replace('\\', '/');
        clean = new File(clean).getName();
        File file = new File(uploadDirectory, clean);
        String parent = uploadDirectory.getCanonicalPath() + File.separator;
        if (!file.getCanonicalPath().startsWith(parent) || !file.exists()) return null;
        return file;
    }

    private Response html(String body) {
        Response response = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", body);
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("X-Content-Type-Options", "nosniff");
        return response;
    }

    private Response text(Response.Status status, String body) {
        return newFixedLengthResponse(status, "text/plain; charset=utf-8", body == null ? "" : body);
    }

    private String renderPage() {
        StringBuilder rows = new StringBuilder();
        List<File> files = FileUtils.listUploadsForDirectory(uploadDirectory);
        if (files.isEmpty()) {
            rows.append("<div class='empty'>尚无上传文件</div>");
        } else {
            for (File file : files) {
                rows.append("<div class='row' data-name=\"").append(attr(file.getName())).append("\">")
                        .append("<div class='info'><b>").append(htmlEscape(file.getName())).append("</b><small>")
                        .append(FileUtils.formatSize(file.length())).append("</small></div>")
                        .append("<button onclick=\"act(this.parentNode.dataset.name,'open')\">")
                        .append(file.getName().toLowerCase(java.util.Locale.US).endsWith(".apk") ? "在电视安装" : "在电视打开")
                        .append("</button><button class='danger' onclick=\"removeFile(this.parentNode.dataset.name)\">永久删除</button></div>");
            }
        }

        String path = htmlEscape(uploadDirectory.getAbsolutePath());
        return "<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>TV 文件盒</title><style>" +
                "*{box-sizing:border-box}body{margin:0;background:#10151c;color:#f5f7fa;font-family:sans-serif}" +
                ".wrap{max-width:820px;margin:auto;padding:22px}.card{background:#19222d;border-radius:14px;padding:20px;margin-bottom:18px}" +
                "h1{margin:0 0 12px;font-size:25px}.path{color:#aeb9c5;word-break:break-all;font-size:14px}" +
                "input{width:100%;padding:15px;background:#222e3b;color:white;border:1px solid #536272;border-radius:8px}" +
                "button{border:0;border-radius:8px;background:#168fd1;color:white;padding:11px 14px;font-weight:bold;margin-left:8px}" +
                "button.danger{background:#b83f4a}.progress{height:8px;background:#263442;border-radius:4px;margin-top:14px;overflow:hidden}" +
                ".bar{height:100%;width:0;background:#35b7ff}.status{margin-top:10px;color:#aeb9c5;min-height:22px}" +
                ".row{display:flex;align-items:center;background:#19222d;padding:13px 14px;border-radius:9px;margin:8px 0}" +
                ".info{flex:1;min-width:0}.info b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.info small{color:#aeb9c5}" +
                ".empty{text-align:center;color:#aeb9c5;padding:30px}@media(max-width:600px){.row{flex-wrap:wrap}.info{width:100%;flex-basis:100%;margin-bottom:10px}.row button{margin:0 8px 0 0}}" +
                "</style></head><body><div class='wrap'><div class='card'><h1>上传文件到电视</h1>" +
                "<div class='path'>文件保存完整路径：" + path + "</div><p><input id='pick' type='file' multiple></p>" +
                "<div class='progress'><div class='bar' id='bar'></div></div><div class='status' id='status'>请选择文件</div></div>" +
                "<h2>已上传文件</h2><div id='files'>" + rows + "</div></div><script>" +
                "const token='" + token + "';const pick=document.getElementById('pick'),bar=document.getElementById('bar'),status=document.getElementById('status');" +
                "pick.onchange=async()=>{for(const f of pick.files){await upload(f)}setTimeout(()=>location.reload(),500)};" +
                "function upload(f){return new Promise(resolve=>{const x=new XMLHttpRequest(),d=new FormData();d.append('file',f);" +
                "x.open('POST','/upload?token='+token+'&filename='+encodeURIComponent(f.name));x.upload.onprogress=e=>{if(e.lengthComputable)bar.style.width=(e.loaded/e.total*100)+'%';status.textContent='正在上传 '+f.name+' '+Math.round(e.loaded/e.total*100)+'%'};" +
                "x.onload=()=>{status.textContent=x.responseText;resolve()};x.onerror=()=>{status.textContent='网络错误';resolve()};x.send(d)})}" +
                "function act(name,action){fetch('/action?token='+token,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action='+action+'&name='+encodeURIComponent(name)}).then(r=>r.text()).then(t=>alert(t))}" +
                "function removeFile(name){if(confirm('永久删除 '+name+'？此操作无法撤销。'))fetch('/action?token='+token,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action=delete&name='+encodeURIComponent(name)}).then(r=>r.text()).then(t=>{alert(t);location.reload()})}" +
                "</script></body></html>";
    }

    private String htmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String attr(String value) {
        return htmlEscape(value).replace("`", "&#96;");
    }
}
