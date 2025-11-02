package com.android.photoviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    TextView textView;

    String site_url = "https://doyoung.pythonanywhere.com"; //배포 서버
    //String site_url = "http://10.0.2.2:8000"; //개발 서버
    //String site_url = "http://192.168.45.206:8000"; //socket_server test

    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";

    CloadImage taskDownload;
    PutPost taskUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);

        // ✅ HTTPS 인증서 검증 무시 (PythonAnywhere HTTPS 접근용 / 실습용)
        try {
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            }}, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ✅ '동기화' 버튼 클릭 시: 서버에서 이미지 목록 불러오기 */
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING)
            taskDownload.cancel(true);
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(this, "Download 시작", Toast.LENGTH_SHORT).show();
    }

    /** ✅ '새로운 이미지 게시' 버튼 클릭 시: 이미지 업로드 */
    public void onClickUpload(View v) {
        File imageFile = new File("/storage/emulated/0/DCIM/Camera/sample.jpg");
        if (!imageFile.exists()) {
            Toast.makeText(this, "이미지 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        taskUpload = new PutPost(imageFile, "안드로이드에서 올린 테스트 게시물");
        taskUpload.execute(site_url + "/api_root/Post/");
    }

    /** ✅ AsyncTask ① : 이미지 + 게시물 목록 다운로드 (GET) */
    private class CloadImage extends AsyncTask<String, Integer, List<PostItem>> {
        @Override
        protected List<PostItem> doInBackground(String... urls) {
            List<PostItem> postList = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL urlAPI = new URL(urls[0]);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    JSONArray aryJson = new JSONArray(result.toString());

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = (JSONObject) aryJson.get(i);
                        int id = postJson.getInt("id");
                        String title = postJson.optString("title", "");
                        String text = postJson.optString("text", "");
                        String imageUrl = postJson.optString("image", "");
                        String publishedDate = postJson.optString("published_date", "");
                        int likeCount = postJson.optInt("like_count", 0);

                        postList.add(new PostItem(id, title, text, imageUrl, publishedDate, likeCount));
                    }
                } else {
                    System.out.println("GET 실패: " + conn.getResponseCode());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return postList;
        }

        @Override
        protected void onPostExecute(List<PostItem> posts) {
            if (posts.isEmpty()) {
                textView.setText("불러올 게시물이 없습니다.");
            } else {
                textView.setText("게시물 로드 성공!");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(new PostAdapter(posts, site_url));
            }
        }
    }

    /** ✅ AsyncTask ② : 이미지 업로드 (POST) */
    private class PutPost extends AsyncTask<String, Integer, String> {
        private final File imageFile;
        private final String title;

        PutPost(File imageFile, String title) {
            this.imageFile = imageFile;
            this.title = title;
        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            String responseMsg = "";

            try {
                String apiUrl = urls[0];
                String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();

                URL url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // 제목(title)
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.writeBytes(title + "\r\n");

                // 이미지 파일
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageFile.getName() + "\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                FileInputStream fis = new FileInputStream(imageFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED)
                    responseMsg = "이미지 업로드 성공!";
                else
                    responseMsg = "업로드 실패 (code: " + responseCode + ")";

            } catch (Exception e) {
                e.printStackTrace();
                responseMsg = "업로드 중 오류 발생: " + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }

            return responseMsg;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }

    }

    public void onClickGoToUpload(View v) {
        Intent intent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(intent);
    }

}
