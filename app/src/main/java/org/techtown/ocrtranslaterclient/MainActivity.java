package org.techtown.ocrtranslaterclient;


import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.audiofx.DynamicsProcessing;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.techtown.ocrtranslaterclient.Configs;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 0;
    public SocketChannel socketChannel = null;
    private ImageView imageView;
    private TextView textView;
    private Button button,button2,button3;
    int PICK_IMAGE_MULTIPLE = 1;
    ArrayList<Drawable> images = new ArrayList<Drawable>();
    ArrayList<Uri> imagesUri = new ArrayList<Uri>();
    ArrayList<Bitmap> imagesfile = new ArrayList<Bitmap>();
    int imageIndex;
    Bitmap imgfile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageIndex++;
                if(imageIndex >= images.size())
                    imageIndex = 0;
                imageView.setImageDrawable(images.get(imageIndex));
            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE_MULTIPLE);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgfile = imagesfile.get(imageIndex);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendFile(imgfile);
                    }
                }).start();
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectServer();
            }
        });
    }

    private String getRealPathFromURI(Uri contentUri) { if (contentUri.getPath().startsWith("/storage")) { return contentUri.getPath(); } String id = DocumentsContract.getDocumentId(contentUri).split(":")[1]; String[] columns = { MediaStore.Files.FileColumns.DATA }; String selection = MediaStore.Files.FileColumns._ID + " = " + id; Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null); try { int columnIndex = cursor.getColumnIndex(columns[0]); if (cursor.moveToFirst()) { return cursor.getString(columnIndex); } } finally { cursor.close(); } return null; }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                if(data.getData()!=null){

                    Uri uri=data.getData();
                    imagesUri.add(uri);
                    images.add(Drawable.createFromStream(
                            getContentResolver().openInputStream(uri),
                            null));
                    Bitmap selPhoto = MediaStore.Images.Media.getBitmap( getContentResolver(), uri );
                    imagesfile.add(selPhoto);

//                realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());

                    Log.i(TAG, "onActivityResult: file path : " + selPhoto);

//                    imagesfile.add(new File(getPath(uri)));
                } else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        ClipData.Item item;
                        Uri uri = null;
                        for (int i = 0; i < mClipData.getItemCount(); i++) {

                            item = mClipData.getItemAt(i);
                            uri = item.getUri();
                            println(uri.toString());
                            imagesUri.add(uri);
                            images.add(Drawable.createFromStream(
                                    getContentResolver().openInputStream(uri),
                                    null));
//                            imagesfile.add(new File(getPath(uri)));
                        }
                        println( "Selected Images" + images.size());
                    }
                }
                imageIndex = 0;
                imageView.setImageDrawable(images.get(imageIndex));
            } else {
                println( "You haven't picked Image");
            }
        } catch (Exception e) {
            e.printStackTrace();
            println("Something went wrong");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void send(String data){
        if(socketChannel == null || !socketChannel.isConnected()){
            connectServer();
        }
        Thread sendingThread = new Thread() {
            @Override
            public void run() {
                ByteBuffer byteBuffer = null; // 바이트 버퍼를 초기화한다.
                Charset charset = Charset.forName("UTF-8");
                byteBuffer = charset.encode(data);
                try {
                    socketChannel.write(byteBuffer);
                } catch(Exception e) {
//					label.setText(label.getText()+"\r\n"+"송신 실패...");
                    println("송신 실패...");
                    return;
                }
//				label.setText(label.getText()+"\r\n"+"송신:" + message);
                println("송신 완료");
                System.out.println("송신 : 길이 " + data.length());
            }
        };
        sendingThread.start();
    }

    public void sendFile(Bitmap file){
        if(socketChannel == null || !socketChannel.isConnected()){
            connectServer();
        }
        Thread fileSendingThread = new Thread() {
            @Override
            public void run() {
                ByteBuffer byteBuffer = null; // 바이트 버퍼를 초기화한다
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        println(file.toString());

                        byteBuffer = ByteBuffer.allocate(file.getByteCount());
                        file.copyPixelsToBuffer(byteBuffer);

                        try {
                            socketChannel.write(byteBuffer);
                        } catch (Exception e) {
                            println("송신 실패...");
                            return;
                        }
                        println("송신 완료");
                        System.out.println("송신 : 길이 " + file.getByteCount());
                    } else {
                        println("버전 오류");
                    }
                } catch (Exception e) {
                    println("파일 오류");
                    return;
                }
            }
        };
        fileSendingThread.start();
    }

    public void connectServer(){
        String ip = Configs.host;
        int port = Configs.port;
        try {
            socketChannel = SocketChannel.open(); // 클라이언트 소켓 채널을 오픈한다.
            socketChannel.configureBlocking(true); // 이 채널을 차단 모드로 둔다.
            println("연결 요청..");
        } catch(Exception e){
            println("연결 요청 실패");
        }
        try {
            socketChannel.connect(new InetSocketAddress(ip, port));
            /* 클라이언트 소켓 채널이 서버에서 설정한 포트 번호와 호스트 네임으로 서버 소켓 채널과 연결을 시도한다. */
            println("연결 성공!");
        } catch (Exception e) {
            println("연결 실패...");
        }
    }

    public String encodeImage(Drawable img){
        println("encode started");
        BitmapDrawable bitimg = (BitmapDrawable) img;
        Bitmap bitmap = bitimg.getBitmap();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        byte[] byteArray = byteStream.toByteArray();
        String baseString = Base64.encodeToString(byteArray,Base64.DEFAULT);
        return baseString;
    }

    private void println(String data){textView.append(data+"\n");}
}