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
    Button btnParied, btnSearch, btnSend, btnPlayDel, btnSongLoad, btnCodeNull;

    Button[] codeBtns = new Button[24];
    Integer[] codeBtnIDs = {R.id.btn_code_c, R.id.btn_code_d, R.id.btn_code_e, R.id.btn_code_f, R.id.btn_code_g, R.id.btn_code_h, R.id.btn_code_i, R.id.btn_code_j, R.id.btn_code_k,
            R.id.btn_code_l, R.id.btn_code_m, R.id.btn_code_n, R.id.btn_code_o, R.id.btn_code_p, R.id.btn_code_cc, R.id.btn_code_dd, R.id.btn_code_ff, R.id.btn_code_gg, R.id.btn_code_hh,
            R.id.btn_code_jj, R.id.btn_code_kk, R.id.btn_code_mm, R.id.btn_code_nn, R.id.btn_code_oo};

    int i;
    Button btnSong1, btnSong2, btnSong3;
    Button btnBeat0, btnBeat1, btnBeat2, btnBeat3, btnBeat4;
    ListView listView;
    String sendStr = "";
    List<Object> showStr = new ArrayList<Object>();

    LinearLayout contentMain;
    LayoutInflater inflater;
    View dlgsongView, dlgbeatView;

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

    public void bluetoothPermissionsChk() {
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

    //---------블루투스 끝---------
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
            codeLongClickBeat(); //박자선택
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

                dlg.setPositiveButton("닫기", null);
                dlg.show();
            }
        });
    }


    //저장곡 불러오기
    public void onClickSong() {
        btnSong1 = (Button) dlgsongView.findViewById(R.id.song1btn);
        btnSong2 = (Button) dlgsongView.findViewById(R.id.song2btn);
        btnSong3 = (Button) dlgsongView.findViewById(R.id.song3btn);
        //팝업에서 노래 클릭했을떄 처리
        View.OnClickListener SongListner = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String songStr;
                switch (view.getId()) {
                    //TODO 노래 추가 넣기(음계)
                    case R.id.song1btn:
                        songStr = "g2g2h2h2g2g2e2 2g2g2e2e2d2 2g2g2h2h2g2g2e2 2g2e2d2e2c2";
                        codeSendstrWithShowstr(songStr);
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(닫기 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song2btn:
                        songStr = "g1g1h1h1g1g1e1 1g1g1e1e1d1 1g1g1h1h1g1g1e1 1g1e1d1e1c1";
                        codeSendstrWithShowstr(songStr);
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(닫기 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.song3btn:
                        songStr = "g2g2h2h2g2g2e2 2g2g2e2";
                        codeSendstrWithShowstr(songStr);
                        Toast.makeText(getApplicationContext(), "불러오기 완료!(닫기 버튼 후 재생버튼 을 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        btnSong1.setOnClickListener(SongListner);
        btnSong2.setOnClickListener(SongListner);
        btnSong3.setOnClickListener(SongListner);

    }

    //박자 팝업 선택
    public void codeLongClickBeat() {
        setCodeBtnIDs();
        for (i = 0; i < codeBtns.length; i++) {
            final int index;
            index = i;
            codeBtns[index].setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    dlgbeatView = (View) View.inflate(MainActivity.this, R.layout.beat_layout, null);

                    AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                    dlg.setTitle("박자 선택");
                    dlg.setView(dlgbeatView);
                    onClickCodeBtnBeat(v.getId()); //클릭한 버튼 id값 넘겨줌

                    dlg.setPositiveButton("닫기", null);
                    dlg.show();

                    return true;
                }
            });
        }
    }

    //박자 버튼 이벤트
    public void onClickCodeBtnBeat(int codeBtnViewID) {
        btnBeat0 = (Button) dlgbeatView.findViewById(R.id.btn_beat0);
        btnBeat1 = (Button) dlgbeatView.findViewById(R.id.btn_beat1);
        btnBeat2 = (Button) dlgbeatView.findViewById(R.id.btn_beat2);
        btnBeat3 = (Button) dlgbeatView.findViewById(R.id.btn_beat3);
        btnBeat4 = (Button) dlgbeatView.findViewById(R.id.btn_beat4);
        View.OnClickListener BeatListner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_beat0:
                        codeSendstrWithShowstr(codeBtnValue(codeBtnViewID, 0));
                        Toast.makeText(getApplicationContext(), "닫기를 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_beat1:
                        codeSendstrWithShowstr(codeBtnValue(codeBtnViewID, 1));
                        Toast.makeText(getApplicationContext(), "닫기를 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_beat2:
                        codeSendstrWithShowstr(codeBtnValue(codeBtnViewID, 2));
                        Toast.makeText(getApplicationContext(), "닫기를 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_beat3:
                        codeSendstrWithShowstr(codeBtnValue(codeBtnViewID, 3));
                        Toast.makeText(getApplicationContext(), "닫기를 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_beat4:
                        codeSendstrWithShowstr(codeBtnValue(codeBtnViewID, 4));
                        Toast.makeText(getApplicationContext(), "닫기를 눌러주세요)", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        btnBeat0.setOnClickListener(BeatListner);
        btnBeat1.setOnClickListener(BeatListner);
        btnBeat2.setOnClickListener(BeatListner);
        btnBeat3.setOnClickListener(BeatListner);
        btnBeat4.setOnClickListener(BeatListner);
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
                codeSendstrWithShowstr(" 2");
            }
        });
        //쉼표 박자 선택
        btnCodeNull.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dlgbeatView = (View) View.inflate(MainActivity.this, R.layout.beat_layout, null);

                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("박자 선택");
                dlg.setView(dlgbeatView);
                onClickCodeBtnBeat(v.getId()); //클릭한 버튼 id값 넘겨줌

                dlg.setPositiveButton("닫기", null);
                dlg.show();

                return true;
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

        //한음씩 지우기
        btnPlayDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendStr에 값을 하나씩 지우고 textview 표시
                if (!sendStr.isEmpty()) {
                    sendStr = sendStr.substring(0, sendStr.length() - 1);
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
            public boolean onLongClick(View v) {
                if (!sendStr.isEmpty()) {
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

    //코드 연결 확인 및 전송
    public void btChkSend(String code) {
        if (btContflag) {
            connectedThread.write(code);
            Toast.makeText(getApplicationContext(), code, Toast.LENGTH_SHORT).show();
        } else {
            //TODO 토스트출력시간 줄이기 0.5쯤
            Toast.makeText(getApplicationContext(), "기기가 연결되어있지 않습니다!", Toast.LENGTH_SHORT).show();
        }
    }

    //코드값을 sendStr에 기록하고, 화면 표시글 처리
    public void codeSendstrWithShowstr(String code) {
        sendStr = sendStr + code;
        //받은 code에서 음과 박자 분리
        for (int codeSize = 0; codeSize < code.length(); codeSize += 2) {
            String codeVal = String.valueOf(code.charAt(codeSize));
            int beatVal = Character.getNumericValue(code.charAt(codeSize + 1));

            showStr.add(codeToCodeNameStrValue(codeVal, beatVal));
        }
        codetextview.setText(showStr.toString());
    }

    //버튼코드값을 화면표시를 위해 변환 처리
    public String codeToCodeNameStrValue(String code, int beat) {
        switch (code) {
            case "c":
                return "도" + beat;
            case "C":
                return "도#" + beat;
            case "d":
                return "레" + beat;
            case "D":
                return "레#" + beat;
            case "e":
                return "미" + beat;
            case "f":
                return "파" + beat;
            case "F":
                return "파#" + beat;
            case "g":
                return "솔" + beat;
            case "G":
                return "솔#" + beat;
            case "h":
                return "라" + beat;
            case "H":
                return "라#" + beat;
            case "i":
                return "시" + beat;
            case "j":
                return "높은 도" + beat;
            case "J":
                return "높은 도#" + beat;
            case "k":
                return "높은 레" + beat;
            case "K":
                return "높은 레#" + beat;
            case "l":
                return "높은 미" + beat;
            case "m":
                return "높은 파" + beat;
            case "M":
                return "높은 파#" + beat;
            case "n":
                return "높은 솔" + beat;
            case "N":
                return "높은 솔#" + beat;
            case "o":
                return "높은 라" + beat;
            case "O":
                return "높은 라#" + beat;
            case "p":
                return "높은 시" + beat;
            case " ":
                return "_" + beat;
        }
        return null;
    }


    //버튼 클릭시 값 지정
    public String codeBtnValue(int btnid, int beat) {
        switch (btnid) {
            case R.id.btn_code_c:
                return "c" + beat;
            case R.id.btn_code_cc:
                return "C" + beat;
            case R.id.btn_code_d:
                return "d" + beat;
            case R.id.btn_code_dd:
                return "D" + beat;
            case R.id.btn_code_e:
                return "e" + beat;
            case R.id.btn_code_f:
                return "f" + beat;
            case R.id.btn_code_ff:
                return "F" + beat;
            case R.id.btn_code_g:
                return "g" + beat;
            case R.id.btn_code_gg:
                return "G" + beat;
            case R.id.btn_code_h:
                return "h" + beat;
            case R.id.btn_code_hh:
                return "H" + beat;
            case R.id.btn_code_i:
                return "i" + beat;
            case R.id.btn_code_j:
                return "j" + beat;
            case R.id.btn_code_jj:
                return "J" + beat;
            case R.id.btn_code_k:
                return "k" + beat;
            case R.id.btn_code_kk:
                return "K" + beat;
            case R.id.btn_code_l:
                return "l" + beat;
            case R.id.btn_code_m:
                return "m" + beat;
            case R.id.btn_code_mm:
                return "M" + beat;
            case R.id.btn_code_n:
                return "n" + beat;
            case R.id.btn_code_nn:
                return "N" + beat;
            case R.id.btn_code_o:
                return "o" + beat;
            case R.id.btn_code_oo:
                return "O" + beat;
            case R.id.btn_code_p:
                return "p" + beat;
            case R.id.btn_code_null:
                return " " + beat;
        }
        return null;
    }

    //코드 버튼
    public void setCodeBtnIDs() {
        for (i = 0; i < codeBtnIDs.length; i++) {
            codeBtns[i] = (Button) findViewById(codeBtnIDs[i]);
        }
    }


    //코드 버튼 처리
    public void setCodeListener() {
        setCodeBtnIDs();

        for (i = 0; i < codeBtns.length; i++) {
            final int index;
            index = i;
            codeBtns[index].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (contentMain.getChildCount() > 1) { //연주모드
                        //요것이 눌린 버튼 따라서 해당 버튼의 id나 정보를 가져온뒤 해당 정보에 맞는 값을 전송시키면됨
                        codeSendstrWithShowstr(codeBtnValue(v.getId(), 2));
                    } else { //악보모드
                        btChkSend(codeBtnValue(v.getId(), 2)); //리턴받은 값을 통해 연결 확인 후 전송(기본박자2)
                    }
                }
            });
        }
    }
}