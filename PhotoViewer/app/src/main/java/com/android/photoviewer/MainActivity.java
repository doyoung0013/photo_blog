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

public class MainActivity extends AppCompatActivity {

    TextView textView;
    String site_url = "https://www.pythonanywhere.com/user/doyoung"; // 로컬 Django 서버 주소
    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415"; // 내가 쓰고 있는 토큰으로 교체
    CloadImage taskDownload;
    PutPost taskUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
    }

    /** [동기화 버튼] 클릭 시 서버에서 이미지 목록 불러오기 */
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING)
            taskDownload.cancel(true);
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(this, "Download 시작", Toast.LENGTH_SHORT).show();
    }

    /** [새로운 이미지 게시] 버튼 클릭 시 이미지 업로드 */
    public void onClickUpload(View v) {
        File imageFile = new File("/storage/emulated/0/DCIM/Camera/sample.jpg");
        if (!imageFile.exists()) {
            Toast.makeText(this, "이미지 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        taskUpload = new PutPost(imageFile, "테스트 업로드 from Android");
        taskUpload.execute(site_url + "/api_root/Post/");
    }

    /** --------------------- AsyncTask ①: 이미지 다운로드 --------------------- */
    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL urlAPI = new URL(urls[0]);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

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

                    // 모든 게시글의 이미지 다운로드
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = (JSONObject) aryJson.get(i);
                        String imageUrl = postJson.getString("image");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            URL imgUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) imgUrl.openConnection();
                            InputStream imgStream = imgConn.getInputStream();
                            Bitmap bmp = BitmapFactory.decodeStream(imgStream);
                            bitmapList.add(bmp);
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(new ImageAdapter(images));
            }
        }
    }

    /** --------------------- AsyncTask ②: 이미지 업로드 --------------------- */
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
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
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
}
