package com.example.ragamfinal.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends Request<NetworkResponse> {
    private static final String TAG = "MultipartRequest";
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    
    private Response.Listener<NetworkResponse> mListener;
    private Response.ErrorListener mErrorListener;
    private Map<String, String> mHeaders;
    private Map<String, String> mParams;
    private Map<String, FileItem> mFiles;
    private Context context;
    
    public static class FileItem {
        public String fileName;
        public String mimeType;
        public Uri uri;
        
        public FileItem(String fileName, String mimeType, Uri uri) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.uri = uri;
        }
    }
    
    public MultipartRequest(String url, Context context,
                           Response.ErrorListener errorListener,
                           Response.Listener<NetworkResponse> listener) {
        super(Method.POST, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.context = context;
        this.mHeaders = new HashMap<>();
        this.mParams = new HashMap<>();
        this.mFiles = new HashMap<>();
        
        // Set timeout for large file uploads
        setRetryPolicy(new DefaultRetryPolicy(
            60000, // 60 seconds timeout
            0, // No retries
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }
    
    public void addStringParam(String key, String value) {
        mParams.put(key, value);
    }
    
    public void addFileParam(String key, String fileName, String mimeType, Uri uri) {
        mFiles.put(key, new FileItem(fileName, mimeType, uri));
    }
    
    @Override
    public Map<String, String> getHeaders() {
        return mHeaders;
    }
    
    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }
    
    @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        
        try {
            // Add string parameters
            for (Map.Entry<String, String> entry : mParams.entrySet()) {
                buildTextPart(dos, entry.getKey(), entry.getValue());
            }
            
            // Add file parameters
            for (Map.Entry<String, FileItem> entry : mFiles.entrySet()) {
                buildFilePart(dos, entry.getKey(), entry.getValue());
            }
            
            // Close multipart form data after file data
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            
            return bos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error building multipart request", e);
            return null;
        }
    }
    
    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        dataOutputStream.writeBytes(parameterValue + lineEnd);
    }
    
    private void buildFilePart(DataOutputStream dataOutputStream, String parameterName, FileItem fileItem) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"; filename=\"" + fileItem.fileName + "\"" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: " + fileItem.mimeType + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        
        // Read file from URI and write to output stream
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileItem.uri)) {
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file from URI: " + fileItem.uri, e);
            throw e;
        }
        
        dataOutputStream.writeBytes(lineEnd);
    }
    
    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new VolleyError(e));
        }
    }
    
    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }
    
    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }
    
    public static String getFileName(Context context, Uri uri) {
        String fileName = "video.mp4";
        
        try {
            if ("content".equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                }
            } else if ("file".equals(uri.getScheme())) {
                fileName = uri.getLastPathSegment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name", e);
        }
        
        return fileName;
    }
    
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        
        try {
            mimeType = context.getContentResolver().getType(uri);
            
            // If we can't get the MIME type from content resolver, try to guess from file extension
            if (mimeType == null) {
                String path = uri.getPath();
                if (path != null) {
                    String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                    switch (extension) {
                        case "jpg":
                        case "jpeg":
                            mimeType = "image/jpeg";
                            break;
                        case "png":
                            mimeType = "image/png";
                            break;
                        case "gif":
                            mimeType = "image/gif";
                            break;
                        case "mp4":
                            mimeType = "video/mp4";
                            break;
                        default:
                            mimeType = "application/octet-stream";
                            break;
                    }
                }
            }
            
            // Final fallback
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mime type", e);
            mimeType = "application/octet-stream";
        }
        
        Log.d(TAG, "Detected MIME type: " + mimeType + " for URI: " + uri);
        return mimeType;
    }
}
