package com.android.photoviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadActivity extends AppCompatActivity {

    EditText editTitle, editText;
    ImageView imagePreview;
    Uri selectedImageUri;

    String site_url = "https://doyoung.pythonanywhere.com";
    //String site_url = "http://10.0.2.2:8000"; //개발 서버
    //String site_url = "http://192.168.45.206:8000"; //socket_server test
    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        editTitle = findViewById(R.id.editTitle);
        editText = findViewById(R.id.editText);
        imagePreview = findViewById(R.id.imagePreview);
    }

    /** 이미지 선택 버튼 클릭 */
    public void onClickSelectImage(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1000);
    }

    /** 선택된 이미지 표시 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imagePreview.setImageURI(selectedImageUri);
        }
    }

    /** 게시하기 버튼 클릭 */
    public void onClickUpload(View v) {
        String title = editTitle.getText().toString();
        String text = editText.getText().toString();

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "이미지를 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        new UploadTask(title, text).execute(site_url + "/api_root/Post/");
    }

    /** 이미지 + 제목 + 내용 업로드 */
    private class UploadTask extends AsyncTask<String, Integer, String> {
        private final String title;
        private final String text;

        UploadTask(String title, String text) {
            this.title = title;
            this.text = text;
        }

        @Override
        protected String doInBackground(String... urls) {
            String responseMsg = "";
            HttpURLConnection conn = null;
            try {
                File imageFile = new File(FileUtils.getPath(UploadActivity.this, selectedImageUri));
                String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();

                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // title 필드
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.writeBytes(title + "\r\n");

                // text 필드
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                dos.writeBytes(text + "\r\n");

                // image 필드
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
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    responseMsg = "게시 성공!";
                } else {
                    responseMsg = "업로드 실패 (" + responseCode + ")";
                }

            } catch (Exception e) {
                e.printStackTrace();
                responseMsg = "에러: " + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }

            return responseMsg;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(UploadActivity.this, result, Toast.LENGTH_LONG).show();
            if (result.contains("성공")) finish();  // 성공 시 화면 닫기
        }
    }
}
