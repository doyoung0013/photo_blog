package com.android.photoviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";
    EditText editTitle, editText;
    ImageView imagePreview;
    Uri selectedImageUri;

    String site_url = "https://doyoung.pythonanywhere.com";
    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        editTitle = findViewById(R.id.editTitle);
        editText = findViewById(R.id.editText);
        imagePreview = findViewById(R.id.imagePreview);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
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
            Log.d(TAG, "이미지 선택됨: " + selectedImageUri.toString());
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

    /** 이미지 MIME 타입 가져오기 */
    private String getMimeType(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = cr.getType(uri);
        if (type == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            type = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type != null ? type : "image/jpeg";
    }

    /** 파일명 생성 */
    private String getFileName(Uri uri) {
        String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
        try {
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                if (nameIndex != -1) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.isEmpty()) {
                        fileName = name;
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "파일명 가져오기 실패: " + e.getMessage());
        }
        return fileName;
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
            InputStream inputStream = null;
            DataOutputStream dos = null;

            try {
                String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                // Content URI에서 직접 InputStream 가져오기
                inputStream = getContentResolver().openInputStream(selectedImageUri);
                if (inputStream == null) {
                    return "이미지를 읽을 수 없습니다.";
                }

                String fileName = getFileName(selectedImageUri);
                String mimeType = getMimeType(selectedImageUri);

                Log.d(TAG, "업로드 시작 - 파일명: " + fileName + ", MIME: " + mimeType);

                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                // title 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(title + lineEnd);

                // text 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(text + lineEnd);

                // image 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: " + mimeType + lineEnd);
                dos.writeBytes(lineEnd);

                // 이미지 데이터 전송
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                Log.d(TAG, "이미지 전송 완료: " + totalBytesRead + " bytes");

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();

                // 응답 확인
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "응답 코드: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    responseMsg = "게시 성공!";
                } else {
                    // 에러 응답 본문 읽기
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                        String errorResponse = s.hasNext() ? s.next() : "";
                        Log.e(TAG, "서버 에러 응답: " + errorResponse);
                        errorStream.close();
                    }
                    responseMsg = "업로드 실패 (코드: " + responseCode + ")";
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "업로드 에러", e);
                responseMsg = "에러: " + e.getMessage();
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (dos != null) dos.close();
                    if (conn != null) conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "리소스 정리 에러", e);
                }
            }

            return responseMsg;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(UploadActivity.this, result, Toast.LENGTH_LONG).show();
            if (result.contains("성공")) {
                finish();  // 성공 시 화면 닫기
            }
        }
    }
}