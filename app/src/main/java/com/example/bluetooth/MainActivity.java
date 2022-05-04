package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
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

    TextView textStatus;
    Button btnParied, btnSearch, btnlive, btnplay, btnSend;
    Button btnCodeC, btnCodeD,btnCodeE,btnCodeF,btnCodeG,btnCodeH,btnCodeI,btnCodeJ,btnCodeL,btnCodeM,btnCodeN,btnCodeO;
    ListView listView;


    LinearLayout contentMain;
    LayoutInflater inflater;



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

        btnlive = (Button) findViewById(R.id.btn_live);
        btnplay = (Button) findViewById(R.id.btn_play);
        listView = (ListView) findViewById(R.id.listview);

        //layout
        contentMain = findViewById(R.id.content_main);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //음 버튼
        this.initializeView();


        // Show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());
    }


    //블투투스 기기리스트 버튼 이벤트
    public void onClickButtonPaired(View view){
        //레이아웃 보여주기
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);


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
        //레이아웃 변경
        contentMain.removeAllViews();
        contentMain.addView(listView);
        listView.setVisibility(View.VISIBLE);

        // Check if the device is already discovering
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
        //블루투스 리스트, 연주모드, 악보모드 3가지중 하나 레이아웃만 출력!
        //TODO 블루투스 버튼 2개 눌럿을때 다른레이아웃 비활성화 확인!!
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
        } else if (view.getId() == R.id.loadbtn){
            //연주모드 안에 로딩레이아웃 활성화
            listView.setVisibility(View.INVISIBLE);
            //일시적으로만 song레이아웃을 출력후 거기에 버튼 클릭시 빠저나오기 즉 팝업 레이아웃
        }
    }

    //악보모드에서 저장된 노래 레스트 팝업 및 버튼들 값(노래코드) 출력해주기
    public void onClickSong(){

    }

    public void initializeView()
    {
        btnCodeC = (Button) contentMain.findViewById (R.id.btn_code_c);
        btnCodeD = (Button) contentMain.findViewById (R.id.btn_code_d);
        btnCodeE = (Button) contentMain.findViewById (R.id.btn_code_e);
        
    }

    public void setCodeListener() {

        /* 각버튼별로 오버라이딩 하는 방법
        //변수초기화
        initializeView();

        btnCodeC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "도", Toast.LENGTH_SHORT).show();
                //connectedThread.write("c");
            }
        });
        */

        //변수 초기화
        initializeView();

        View.OnClickListener Listner = new View.OnClickListener(){
            @Override
            public void onClick(View view){
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
                        //TODO 버튼별로 모두 추가하기(연결 테스트후)

                }
            }
        };

        btnCodeC.setOnClickListener(Listner);
        btnCodeD.setOnClickListener(Listner);
        btnCodeE.setOnClickListener(Listner);

    }
}