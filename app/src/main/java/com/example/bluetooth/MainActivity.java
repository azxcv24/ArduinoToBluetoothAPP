package com.example.bluetooth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView textStatus, codetextview;
    Button btnParied, btnSearch, btnSend, btnPlayDel, btnSongLoad;
    Button btnCodeC, btnCodeD, btnCodeE, btnCodeF, btnCodeG, btnCodeH, btnCodeI, btnCodeJ, btnCodeK, btnCodeL, btnCodeM, btnCodeN, btnCodeO, btnCodeP, btnCodeNull; //음 버튼
    Button btnCodeCC, btnCodeDD, btnCodeFF, btnCodeGG, btnCodeHH, btnCodeJJ, btnCodeKK, btnCodeMM, btnCodeNN, btnCodeOO; //반음올림 버튼
    Button btnSong1, btnSong2, btnSong3;
    ListView listView;
    String sendStr = "";
    List<Object> showStr = new ArrayList<Object>();

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
    boolean btContflag;

    ProgressDialog customProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //권한 획득
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);
        bluetoothPermissionsChk();

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

        //로딩창
        customProgressDialog = new ProgressDialog(this);
        customProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        //음계 버튼
        this.initializeView();

        // Show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());
    }


    //블투투스 기기리스트 버튼 이벤트
    public void onClickButtonPaired(View view) {
        //레이아웃 처리
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);

        //블루투스
        btArrayAdapter.clear();
        if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
            deviceAddressArray.clear();
        }
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
    public void onClickButtonSearch(View view) {
        //레이아웃 처리
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);

        // Check if the device is already discovering
        //TODO 확인필요 비정상 종료 발생!
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            bluetoothPermissionsChk();
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (btAdapter.isDiscovering()) {
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
            customProgressDialog.show(); //로딩창
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position) + "에 연결을 시도합니다", Toast.LENGTH_SHORT).show();

            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            btContflag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();

            } catch (IOException e) {
                btContflag = false;
                textStatus.setText("연결실패!");
                e.printStackTrace();
            }
            customProgressDialog.dismiss(); //로딩레이아웃 종료
            
            // start bluetooth communication
            if (btContflag) {
                textStatus.setText(name + " 에 연결됨");
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
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    public void bluetoothPermissionsChk(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH

                    },
                    1);

        }
    }


    //모드별 활성화
    public void onClickButtonMode(View view) {
        if (view.getId() == R.id.btn_live) {
            //연주모드
            listView.setVisibility(View.INVISIBLE);
            contentMain.removeAllViews();
            inflater.inflate(R.layout.code_layout, contentMain, true); //자식레이아웃 삽입
            setCodeListener(); //자식레이아웃 추가후 그 레아아웃에 있는 버튼의 클릭리스너 메소드 불러오기
        } else if (view.getId() == R.id.btn_play) {
            //악보모드
            listView.setVisibility(View.INVISIBLE);
            contentMain.removeAllViews();
            inflater.inflate(R.layout.code_layout, contentMain, true); //자식레이아웃 삽입
            inflater.inflate(R.layout.play_layout, contentMain, true);//악보용 레이아웃 추가
            setCodeListener(); //자식레이아웃 추가후 그 레아아웃에 있는 버튼의 클릭리스너 메소드 불러오기
            onPlayBtnListener(); //악보 모드용 버튼 이벤트
            onClickSongLoad(); //노래 리스트 버튼 이벤트
        }
    }

    public void onClickSongLoad() {
        //변수처리
        btnSongLoad = (Button) findViewById(R.id.loadbtn);
        //팝업 다이얼로그
        btnSongLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgsongView = (View) View.inflate(MainActivity.this, R.layout.song_layout, null);

                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("노래 선택");
                dlg.setView(dlgsongView);
                onClickSong(); //노래 이벤트호출

                //버튼 클릭시 동작
                dlg.setPositiveButton("확인", null);
                dlg.setNegativeButton("취소", null);
                dlg.show();
            }
        });
    }


    //저장곡 불러오기
    public void onClickSong() {
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
                        sendStr = "gghhgge ggeed gghhgge gedec"; //전송용
                        showStr.removeAll(showStr);
                        for (int i = 0; i < sendStr.length(); i++) { //표시
                            showStr.add(codeToCodeNameStr(sendStr.charAt(i)));
                        }
                        codetextview.setText(showStr.toString());
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(확인 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song2btn:
                        sendStr = "gghhgge";
                        showStr.removeAll(showStr);
                        for (int i = 0; i < sendStr.length(); i++) { //표시
                            showStr.add(codeToCodeNameStr(sendStr.charAt(i)));
                        }
                        codetextview.setText(showStr.toString());
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(확인 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song3btn:
                        sendStr = "gghhgge";
                        showStr.removeAll(showStr);
                        for (int i = 0; i < sendStr.length(); i++) { //표시
                            showStr.add(codeToCodeNameStr(sendStr.charAt(i)));
                        }
                        codetextview.setText(showStr.toString());
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
        btnCodeNull = (Button) contentMain.findViewById(R.id.btn_code_null);
        btnSend = (Button) contentMain.findViewById(R.id.btn_playsend);
        btnPlayDel = (Button) contentMain.findViewById(R.id.btn_playDelet);
        codetextview = (TextView) contentMain.findViewById(R.id.code_textview);

        //쉼표 버튼 이벤트
        btnCodeNull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStr.add("_");
                codetextview.setText(showStr.toString());
                sendStr = sendStr + " "; //전송용
            }
        });
        //전송
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sendStr.isEmpty()) {
                    if (btContflag) {
                        connectedThread.write(sendStr); //sendStr에 들어있는 문자열을 전송!
                        Toast.makeText(getApplicationContext(), "음계 전송!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "기기가 연결되어있지 않습니다!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "입력된 음계가 없습니다!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //한 음 지우기
        btnPlayDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendStr에 값을 하나씩 지우고 textview 표시
                if (!sendStr.isEmpty()) {
                    sendStr = sendStr.substring(0, sendStr.length() - 1);
                    showStr.remove(showStr.size() - 1);
                    codetextview.setText(showStr.toString());
                } else {
                    Toast.makeText(getApplicationContext(), "계이름을 클릭하세요!", Toast.LENGTH_SHORT).show();
                }
            }
        });        
        //모두 지우기
        btnPlayDel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v){
                if(!sendStr.isEmpty()){
                    Toast.makeText(getApplicationContext(), "기록된 음 모두 제거!", Toast.LENGTH_SHORT).show();
                    sendStr = "";
                    showStr.clear();
                    codetextview.setText(showStr.toString());
                } else {
                    Toast.makeText(getApplicationContext(), "계이름을 클릭하세요!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    public void initializeView() {
        //음계 버튼
        btnCodeC = (Button) contentMain.findViewById(R.id.btn_code_c);
        btnCodeD = (Button) contentMain.findViewById(R.id.btn_code_d);
        btnCodeE = (Button) contentMain.findViewById(R.id.btn_code_e);
        btnCodeF = (Button) contentMain.findViewById(R.id.btn_code_f);
        btnCodeG = (Button) contentMain.findViewById(R.id.btn_code_g);
        btnCodeH = (Button) contentMain.findViewById(R.id.btn_code_h);
        btnCodeI = (Button) contentMain.findViewById(R.id.btn_code_i);
        btnCodeJ = (Button) contentMain.findViewById(R.id.btn_code_j);
        btnCodeK = (Button) contentMain.findViewById(R.id.btn_code_k);
        btnCodeL = (Button) contentMain.findViewById(R.id.btn_code_l);
        btnCodeM = (Button) contentMain.findViewById(R.id.btn_code_m);
        btnCodeN = (Button) contentMain.findViewById(R.id.btn_code_n);
        btnCodeO = (Button) contentMain.findViewById(R.id.btn_code_o);
        btnCodeP = (Button) contentMain.findViewById(R.id.btn_code_p);
        btnCodeCC = (Button) contentMain.findViewById(R.id.btn_code_cc);
        btnCodeDD = (Button) contentMain.findViewById(R.id.btn_code_dd);
        btnCodeFF = (Button) contentMain.findViewById(R.id.btn_code_ff);
        btnCodeGG = (Button) contentMain.findViewById(R.id.btn_code_gg);
        btnCodeHH = (Button) contentMain.findViewById(R.id.btn_code_hh);
        btnCodeJJ = (Button) contentMain.findViewById(R.id.btn_code_jj);
        btnCodeKK = (Button) contentMain.findViewById(R.id.btn_code_kk);
        btnCodeMM = (Button) contentMain.findViewById(R.id.btn_code_mm);
        btnCodeNN = (Button) contentMain.findViewById(R.id.btn_code_nn);
        btnCodeOO = (Button) contentMain.findViewById(R.id.btn_code_oo);
    }

    //코드 표시용 변환 
    public void codeSetShowStr(char code) {
        sendStr = sendStr + code;
        Log.v("sendStr","출력 : "+sendStr);
        showStr.add(codeToCodeNameStr(code));
        codetextview.setText(showStr.toString());
    }

    public String codeToCodeNameStr(char code) {
        switch (code) {
            case 'c':
                return "도";
            case 'C':
                return "도#";
            case 'd':
                return "레";
            case 'D':
                return "레#";
            case 'e':
                return "미";
            case 'f':
                return "파";
            case 'F':
                return "파#";
            case 'g':
                return "솔";
            case 'G':
                return "솔#";
            case 'h':
                return "라";
            case 'H':
                return "라#";
            case 'i':
                return "시";
            case 'j':
                return "높은 도";
            case 'J':
                return "높은 도#";
            case 'k':
                return "높은 레";
            case 'K':
                return "높은 레#";
            case 'l':
                return "높은 미";
            case 'm':
                return "높은 파";
            case 'M':
                return "높은 파#";
            case 'n':
                return "높은 솔";
            case 'N':
                return "높은 솔#";
            case 'o':
                return "높은 라";
            case 'O':
                return "높은 라#";
            case 'p':
                return "높은 시";
            case ' ':
                return "_";
        }
        return null;
    }


    //코드 연결 확인 및 전송
    public void btChkSend(char code) {
        if (btContflag) {
            connectedThread.write(Character.toString(code));
            Toast.makeText(getApplicationContext(), codeToCodeNameStr(code), Toast.LENGTH_SHORT).show();
        } else {
            //TODO 토스트출력시간 줄이기 0.5쯤
            Toast.makeText(getApplicationContext(), "기기가 연결되어있지 않습니다!", Toast.LENGTH_SHORT).show();
        }
    }


    //코드 버튼 처리
    public void setCodeListener() {
        //변수 초기화
        initializeView();

        //버튼 이벤트
        View.OnClickListener codeListner = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contentMain.getChildCount() > 1) { //contentMain에 추가되는 자식 카운트(연주모드에선 2개 들어감)
                    //악보모드

                    switch (view.getId()) {
                        case R.id.btn_code_c:
                            codeSetShowStr('c');
                            break;
                        case R.id.btn_code_cc:
                            codeSetShowStr('C');
                            break;
                        case R.id.btn_code_d:
                            codeSetShowStr('d');
                            break;
                        case R.id.btn_code_dd:
                            codeSetShowStr('D');
                            break;
                        case R.id.btn_code_e:
                            codeSetShowStr('e');
                            break;
                        case R.id.btn_code_f:
                            codeSetShowStr('f');
                            break;
                        case R.id.btn_code_ff:
                            codeSetShowStr('F');
                            break;
                        case R.id.btn_code_g:
                            codeSetShowStr('g');
                            break;
                        case R.id.btn_code_gg:
                            codeSetShowStr('G');
                            break;
                        case R.id.btn_code_h:
                            codeSetShowStr('h');
                            break;
                        case R.id.btn_code_hh:
                            codeSetShowStr('H');
                            break;
                        case R.id.btn_code_i:
                            codeSetShowStr('i');
                            break;
                        case R.id.btn_code_j:
                            codeSetShowStr('j');
                            break;
                        case R.id.btn_code_jj:
                            codeSetShowStr('J');
                            break;
                        case R.id.btn_code_k:
                            codeSetShowStr('k');
                            break;
                        case R.id.btn_code_kk:
                            codeSetShowStr('K');
                            break;
                        case R.id.btn_code_l:
                            codeSetShowStr('l');
                            break;
                        case R.id.btn_code_m:
                            codeSetShowStr('m');
                            break;
                        case R.id.btn_code_mm:
                            codeSetShowStr('M');
                            break;
                        case R.id.btn_code_n:
                            codeSetShowStr('n');
                            break;
                        case R.id.btn_code_nn:
                            codeSetShowStr('N');
                            break;
                        case R.id.btn_code_o:
                            codeSetShowStr('o');
                            break;
                        case R.id.btn_code_oo:
                            codeSetShowStr('O');
                            break;
                        case R.id.btn_code_p:
                            codeSetShowStr('p');
                            break;
                    }
                } else {
                    //일반적 출력
                    switch (view.getId()) {
                        case R.id.btn_code_c:
                            btChkSend('c');
                            break;
                        case R.id.btn_code_cc:
                            btChkSend('C');
                            break;
                        case R.id.btn_code_d:
                            btChkSend('d');
                            break;
                        case R.id.btn_code_dd:
                            btChkSend('D');
                            break;
                        case R.id.btn_code_e:
                            btChkSend('e');
                            break;
                        case R.id.btn_code_f:
                            btChkSend('f');
                            break;
                        case R.id.btn_code_ff:
                            btChkSend('F');
                            break;
                        case R.id.btn_code_g:
                            btChkSend('g');
                            break;
                        case R.id.btn_code_gg:
                            btChkSend('G');
                            break;
                        case R.id.btn_code_h:
                            btChkSend('h');
                            break;
                        case R.id.btn_code_hh:
                            btChkSend('H');
                            break;
                        case R.id.btn_code_i:
                            btChkSend('i');
                            break;
                        case R.id.btn_code_j:
                            btChkSend('j');
                            break;
                        case R.id.btn_code_jj:
                            btChkSend('J');
                            break;
                        case R.id.btn_code_k:
                            btChkSend('k');
                            break;
                        case R.id.btn_code_kk:
                            btChkSend('K');
                            break;
                        case R.id.btn_code_l:
                            btChkSend('l');
                            break;
                        case R.id.btn_code_m:
                            btChkSend('m');
                            break;
                        case R.id.btn_code_mm:
                            btChkSend('M');
                            break;
                        case R.id.btn_code_n:
                            btChkSend('n');
                            break;
                        case R.id.btn_code_nn:
                            btChkSend('N');
                            break;
                        case R.id.btn_code_o:
                            btChkSend('o');
                            break;
                        case R.id.btn_code_oo:
                            btChkSend('O');
                            break;
                        case R.id.btn_code_p:
                            btChkSend('p');
                            break;
                    }
                }
            }
        };

        //버튼 이벤트 연결
        btnCodeC.setOnClickListener(codeListner);
        btnCodeD.setOnClickListener(codeListner);
        btnCodeE.setOnClickListener(codeListner);
        btnCodeF.setOnClickListener(codeListner);
        btnCodeG.setOnClickListener(codeListner);
        btnCodeH.setOnClickListener(codeListner);
        btnCodeI.setOnClickListener(codeListner);
        btnCodeJ.setOnClickListener(codeListner);
        btnCodeK.setOnClickListener(codeListner);
        btnCodeL.setOnClickListener(codeListner);
        btnCodeM.setOnClickListener(codeListner);
        btnCodeN.setOnClickListener(codeListner);
        btnCodeO.setOnClickListener(codeListner);
        btnCodeP.setOnClickListener(codeListner);
        btnCodeCC.setOnClickListener(codeListner);
        btnCodeDD.setOnClickListener(codeListner);
        btnCodeFF.setOnClickListener(codeListner);
        btnCodeGG.setOnClickListener(codeListner);
        btnCodeHH.setOnClickListener(codeListner);
        btnCodeJJ.setOnClickListener(codeListner);
        btnCodeKK.setOnClickListener(codeListner);
        btnCodeMM.setOnClickListener(codeListner);
        btnCodeNN.setOnClickListener(codeListner);
        btnCodeOO.setOnClickListener(codeListner);

    }
}