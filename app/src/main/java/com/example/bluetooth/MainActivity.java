package com.example.bluetooth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView textStatus, codetextview;
    Button btnParied, btnSearch, btnSend, btnPlayDel,btnSongLoad;
    Button btnCodeC, btnCodeD,btnCodeE,btnCodeF,btnCodeG,btnCodeH,btnCodeI,btnCodeJ,btnCodeL,btnCodeM,btnCodeN,btnCodeO;
    Button btnSong1, btnSong2,btnSong3;
    ListView listView;
    String playStr = "";

    LinearLayout contentMain;
    LayoutInflater inflater;
    View dlgsongView;

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //권한 획득
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_CONNECT
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // variables
        textStatus = (TextView) findViewById(R.id.text_status);
        btnParied = (Button) findViewById(R.id.btn_paired);
        btnSearch = (Button) findViewById(R.id.btn_search);
        listView = (ListView) findViewById(R.id.listview);

        //layout
        contentMain = findViewById(R.id.content_main);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //음계 버튼
        this.initializeView();

        // Show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());
    }


    //블투투스 기기리스트 버튼 이벤트
    public void onClickButtonPaired(View view){
        //레이아웃 처리
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);

        //블루투스
        btArrayAdapter.clear();
        if(deviceAddressArray!=null && !deviceAddressArray.isEmpty()){ deviceAddressArray.clear(); }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        }
    }

    //블루투스 검색버튼이벤트
    public void onClickButtonSearch(View view){
        //레이아웃 처리
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);

        // Check if the device is already discovering
        //TODO 확인필요 비정상 종료 발생!
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        } else {
            if (btAdapter.isEnabled()) {
                btAdapter.startDiscovery();
                btArrayAdapter.clear();
                if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
                    deviceAddressArray.clear();
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter);
            } else {
                Toast.makeText(getApplicationContext(), "블루투스가 꺼저있습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
                btArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position) + "에 연결시도" , Toast.LENGTH_SHORT).show();

            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
                textStatus.setText("연결실패!");
                e.printStackTrace();
            }

            // start bluetooth communication
            if(flag){
                textStatus.setText(name + " 에 연결");
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }

        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    //모드별 활성화
    public void onClickButtonMode(View view){
        //TODO 블루투스 버튼 2개(기기,검색) 눌럿을때 다른레이아웃 비활성화 확인!!
        if (view.getId() == R.id.btn_live){
            //연주모드
            listView.setVisibility(View.INVISIBLE);
            contentMain.removeAllViews();
            inflater.inflate(R.layout.code_layout,contentMain,true); //자식레이아웃 삽입
            setCodeListener(); //자식레이아웃 추가후 그 레아아웃에 있는 버튼의 클릭리스너 메소드 불러오기
        } else if (view.getId() == R.id.btn_play){
            //악보모드
            listView.setVisibility(View.INVISIBLE);
            contentMain.removeAllViews();
            inflater.inflate(R.layout.code_layout,contentMain,true); //자식레이아웃 삽입
            inflater.inflate(R.layout.play_layout,contentMain,true);//악보용 레이아웃 추가
            setCodeListener(); //자식레이아웃 추가후 그 레아아웃에 있는 버튼의 클릭리스너 메소드 불러오기
            onPlayBtnListener(); //악보 모드용 버튼 이벤트
            onClickSongLoad(); //노래 리스트 버튼 이벤트
        }
    }

    public void onClickSongLoad(){
        //변수처리
        btnSongLoad = (Button) findViewById(R.id.loadbtn);
        //팝업 다이얼로그
        btnSongLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgsongView = (View) View.inflate(MainActivity.this, R.layout.song_layout,null);

                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("노래 선택");
                dlg.setView(dlgsongView);
                onClickSong(); //노래 이벤트호출

                //버튼 클릭시 동작
                dlg.setPositiveButton("확인",null);
                dlg.setNegativeButton("취소",null);
                dlg.show();
            }
        });
    }


    //저장곡 불러오기
    public void onClickSong(){
        //변수처리
        btnSong1 = (Button) dlgsongView.findViewById(R.id.song1btn);
        btnSong2 = (Button) dlgsongView.findViewById(R.id.song2btn);
        btnSong3 = (Button) dlgsongView.findViewById(R.id.song3btn);

        //팝업에서 노래 클릭했을떄 처리
        View.OnClickListener SongListner = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    //TODO 노래 넣기(음계)
                    case R.id.song1btn:
                        playStr = "abcdddcddcdcdd";
                        codetextview.setText(playStr);

                        Toast.makeText(getApplicationContext(), "불러오기 완료!(확인 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song2btn:
                        playStr = "bbbeefd";
                        codetextview.setText(playStr);
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(확인 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song3btn:
                        playStr = "adfadfwdv";
                        codetextview.setText(playStr);
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(확인 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        btnSong1.setOnClickListener(SongListner);
        btnSong2.setOnClickListener(SongListner);
        btnSong3.setOnClickListener(SongListner);

    }

    //악보모드
    public void onPlayBtnListener() {
        //악보모드 제어 위젯 변수 초기화
        btnSend = (Button)  contentMain.findViewById (R.id.btn_playsend);
        btnPlayDel = (Button) contentMain.findViewById (R.id.btn_playDelet);
        codetextview = (TextView)  contentMain.findViewById (R.id.code_textview);
        //버튼 이벤트
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playStr != null){
                    connectedThread.write(playStr); //playStr에 들어있는 문자열을 전송!
                    Toast.makeText(getApplicationContext(), "음계 전송!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "입력된 음계가 없습니다!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnPlayDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //playstr에 값을 하나씩 지우고 textview에 값 변경
                if (playStr != null) {
                    playStr = playStr.substring(0, playStr.length()-1);
                } else {
                    Toast.makeText(getApplicationContext(), "계이름을 클릭하세요!", Toast.LENGTH_SHORT).show();
                }
                codetextview.setText(playStr);
            }
        });
    }

    public void initializeView()
    {
        //음계 버튼
        btnCodeC = (Button) contentMain.findViewById (R.id.btn_code_c);
        btnCodeD = (Button) contentMain.findViewById (R.id.btn_code_d);
        btnCodeE = (Button) contentMain.findViewById (R.id.btn_code_e);
        //TODO 버튼 추가(테스트후)
        
    }

    
    //코드 버튼 처리
    public void setCodeListener() {
        //변수 초기화
        initializeView();
        
        //버튼 이벤트
        View.OnClickListener codeListner = new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if ( contentMain.getChildCount() > 1 ){ //contentMain에 추가되는 자식 카운트(연주모드에선 2개 들어감)
                    //악보모드 playstr저장
                    switch (view.getId()) {
                        case R.id.btn_code_c:
                            Toast.makeText(getApplicationContext(), "도인데 악보", Toast.LENGTH_SHORT).show();
                            playStr = playStr + "c";
                            codetextview.setText(playStr);
                            break;
                        case R.id.btn_code_d:
                            Toast.makeText(getApplicationContext(), "레-ㅇ", Toast.LENGTH_SHORT).show();
                            playStr = playStr + "d";
                            codetextview.setText(playStr);
                            break;
                        case R.id.btn_code_e:
                            Toast.makeText(getApplicationContext(), "미-", Toast.LENGTH_SHORT).show();
                            playStr = playStr + "e";
                            codetextview.setText(playStr);
                            break;

                    }
                } else {
                    //일반적 출력
                    switch (view.getId()) {
                        case R.id.btn_code_c:
                            //connectedThread.write("c");
                            Toast.makeText(getApplicationContext(), "도", Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.btn_code_d:
                            //connectedThread.write("d");
                            Toast.makeText(getApplicationContext(), "레", Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.btn_code_e:
                            connectedThread.write("e");
                            Toast.makeText(getApplicationContext(), "미", Toast.LENGTH_SHORT).show();
                            break;
                        //TODO 버튼별로 모두 추가하기(연결 테스트후) 토스트시간 0.5로 변경
                        //TODO 전송전 클릭시 미연결이라면 오류 출력

                    }
                }
            }
        };

        //버튼 이벤트 연결
        btnCodeC.setOnClickListener(codeListner);
        btnCodeD.setOnClickListener(codeListner);
        btnCodeE.setOnClickListener(codeListner);

    }
}