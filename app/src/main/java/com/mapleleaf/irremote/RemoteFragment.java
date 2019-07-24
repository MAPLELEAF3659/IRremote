package com.mapleleaf.irremote;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;


/**
 * A simple {@link Fragment} subclass.
 */
@SuppressWarnings("ALL")
public class RemoteFragment extends Fragment {

    //介面
    private View view;
    private Button wifiButton;
    private ImageView wifiStatusImage;
    private Button btButton;
    private ImageView btStatusImage;
    private Button sendButton;
    private Button addButton;
    private Button editButton;
    private Button alarmButton;
    private Spinner remoteSpinner;

    //藍牙
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Handler bluetoothHandler;
    BluetoothDevice bluetoothDevice;
    ConnectedThread connectedThread;
    private BluetoothSocket bluetoothSocket;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    private String _recieveData = "";
    private boolean btRecv = false;
    private ProgressDialog progress;

    private boolean isNetworkConnected = false;
    private boolean isBluetoothConnected = false;
    private String mode;

    //資料庫
    private DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference setMode = firebaseDatabase.child("mode");
    DatabaseReference temp = firebaseDatabase.child("temp");
    private DatabaseReference testFirebase = FirebaseDatabase.getInstance().getReference(".info/connected");
    List<String> remotesName = new ArrayList<String>();
    static ArrayAdapter<String> nameAdapter;

    public RemoteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_remote, container, false);
        nameAdapter = new ArrayAdapter<String>(getContext().getApplicationContext(), R.layout.spinner_layout, remotesName);
        firebaseDatabase.child("test").setValue("test");
        testFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //noinspection ConstantConditions
                if (dataSnapshot.getValue(Boolean.class)) {
                    wifiButton.setText("已連線");
                    wifiButton.setEnabled(false);
                    wifiStatusImage.setImageResource(R.drawable.baseline_wifi_black_24dp);
                    Snackbar.make(getView(), "已連線至網路", Snackbar.LENGTH_LONG).show();
                    isNetworkConnected = true;
                } else {
                    wifiButton.setText("網路連線");
                    wifiButton.setEnabled(true);
                    wifiStatusImage.setImageResource(R.drawable.baseline_wifi_off_black_24dp);
                    isNetworkConnected = false;
                }
                firebaseDatabase.child("test").removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        for (Map.Entry<String, ?> entry : Prefs.getAll().entrySet()) {
            if (entry.getKey().contains("irdata,")) {
                remotesName.add(entry.getKey().substring(7));
            }
        }
        remoteSpinner = view.findViewById(R.id.remoteSpinner);
        remoteSpinner.setAdapter(nameAdapter);
        wifiStatusImage = view.findViewById(R.id.wifiStatusImage);
        wifiButton = view.findViewById(R.id.wifibutton);
        wifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseDatabase.child("test").setValue("test");
                if (!isNetworkConnected)
                    Toast.makeText(Objects.requireNonNull(getContext()).getApplicationContext(), "嘗試連線(請開啟行動數據或WiFi)", LENGTH_LONG).show();
            }
        });
        btStatusImage = view.findViewById(R.id.bluetoothStatusImage);
        btButton = view.findViewById(R.id.bluetoothButton);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter != null) {
                    if (!bluetoothAdapter.isEnabled()) {//如果藍芽沒開啟
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);//跳出視窗
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        Snackbar.make(getView(), "藍牙已開啟", Snackbar.LENGTH_SHORT).show();
                    } else {
                        if (btButton.getText().equals("藍牙連線")) {
                            btButton.setText("嘗試連線");
                            new ConnectBT().execute();
                        } else {
                            btButton.setText("藍牙連線");
                            bluetoothDisconnect();
                            Snackbar.make(getView(), "已斷開藍牙連線", Snackbar.LENGTH_LONG).show();
                        }
                    }
                } else
                    Snackbar.make(getView(), "裝置不支援藍牙", Snackbar.LENGTH_LONG).show();
            }
        });
        sendButton = view.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar mCal = Calendar.getInstance();
                CharSequence date = DateFormat.format("yyyy/MM/dd", mCal.getTime());
                CharSequence time = DateFormat.format("kk:mm:ss", mCal.getTime());
                if (isBluetoothConnected) {
                    connectedThread.write(Prefs.getString("irdata," + remoteSpinner.getSelectedItem().toString()));
                    HistoryFragment.historyAdd("history," + remoteSpinner.getSelectedItem().toString() + "," + date + "/" + time + ",藍牙", "alarm");
                } else {
                    firebaseDatabase.child("trans").setValue(Prefs.getString("irdata," + remoteSpinner.getSelectedItem().toString()));
                    setMode.setValue("trans");
                    HistoryFragment.historyAdd("history," + remoteSpinner.getSelectedItem().toString() + "," + date + "/" + time + ",WiFi", "alarm");
                }
            }
        });
        addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMode();
            }
        });
        editButton = view.findViewById(R.id.editButton);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                @SuppressLint("InflateParams") final View editView = inflater.inflate(R.layout.edit_add_remote_dialog, null);
                final EditText buttonName = editView.findViewById(R.id.editText);
                buttonName.setHint(remoteSpinner.getSelectedItem().toString());
                final String temp = Prefs.getString("irdata," + remoteSpinner.getSelectedItem().toString());
                new AlertDialog.Builder(getContext())
                        .setTitle("編輯按鍵 : " + remoteSpinner.getSelectedItem().toString())
                        .setView(editView)
                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (buttonName.getText().length() > 0) {
                                    if (!Prefs.contains("irdata," + buttonName.getText().toString())) {
                                        Prefs.remove("irdata," + remoteSpinner.getSelectedItem().toString());
                                        Prefs.putString("irdata," + buttonName.getText().toString(), temp);
                                        remotesName.remove(String.valueOf(remoteSpinner.getSelectedItem().toString()));
                                        remotesName.add(buttonName.getText().toString());
                                        remoteSpinner.setAdapter(nameAdapter);
                                        Toast.makeText(getContext().getApplicationContext(), "已修改名稱為：" + buttonName.getText().toString(), Toast.LENGTH_SHORT).show();
                                    } else
                                        Toast.makeText(getContext().getApplicationContext(), "已存在的按鍵", Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(getContext().getApplicationContext(), "未輸入名稱，取消設定", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNeutralButton("刪除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Prefs.remove("irdata," + remoteSpinner.getSelectedItem().toString());
                                remotesName.remove(remoteSpinner.getSelectedItem().toString());
                                remoteSpinner.setAdapter(nameAdapter);
                                Toast.makeText(getContext().getApplicationContext(), "已刪除按鍵：" + buttonName.getText().toString(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getContext().getApplicationContext(), "取消設定", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Toast.makeText(getContext().getApplicationContext(), "取消設定", Toast.LENGTH_SHORT).show();
                                addMode();
                            }
                        })
                        .show();
            }
        });
        alarmButton = view.findViewById(R.id.alarmButton);
        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(System.currentTimeMillis());
                c.add(Calendar.SECOND, 60 - c.getTime().getSeconds());
                CharSequence time = DateFormat.format("kk:mm:ss", c.getTime());
                Intent intent = new Intent(getContext(), AlarmReceiver.class);
                Bundle bundle = new Bundle();
                bundle.putString("trans", Prefs.getString("irdata," + remoteSpinner.getSelectedItem().toString()));
                bundle.putString("name", remoteSpinner.getSelectedItem().toString());
                intent.putExtras(bundle);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
                Snackbar.make(getView(), "已設定於 " + time + " 自動發送「" + remoteSpinner.getSelectedItem().toString() + "」訊號", Snackbar.LENGTH_LONG).show();
            }
        });
        temp.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if (mode.equals("recv") && !dataSnapshot.getValue(String.class).equals("")) {
                    addRemote(dataSnapshot.getValue(String.class));
                    addMode();
                    FirebaseDatabase.getInstance().getReference().child("temp").setValue("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        setMode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mode = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        bluetoothHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) { //收到MESSAGE_READ 開始接收資料
                    try {
                        _recieveData = new String((byte[]) msg.obj, "UTF-8");
                        if (_recieveData.contains(",") && btRecv) {
                            addRemote(_recieveData);
                        }
                        _recieveData = "";
                        //connectedThread.write("test");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //mReadBuffer.setText(_recieveData); //將收到的字串呈現在畫面上
                }

                if (msg.what == CONNECTING_STATUS) {
                    //收到CONNECTING_STATUS 顯示以下訊息
                    if (msg.arg1 == 1)
                        btButton.setText("已連線");
                    else
                        btButton.setText("藍牙連線");
                }
            }
        };
        return view;
    }

    void addRemote(final String data) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") final View addView = inflater.inflate(R.layout.edit_add_remote_dialog, null);
        final EditText buttonName = addView.findViewById(R.id.editText);
        buttonName.setHint(data);
        new AlertDialog.Builder(getContext())
                .setTitle("已偵測到信號")
                .setView(addView)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (buttonName.getText().length() > 0) {
                            Prefs.putString("irdata," + String.valueOf(buttonName.getText()), data);
                            remotesName.add(String.valueOf(buttonName.getText()));
                            remoteSpinner.setAdapter(nameAdapter);
                            Snackbar.make(getView(), "已新增按鍵：" + buttonName.getText().toString(), Snackbar.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext().getApplicationContext(), "未輸入名稱，設定失敗", Toast.LENGTH_SHORT).show();
                            addMode();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext().getApplicationContext(), "取消設定", Toast.LENGTH_SHORT).show();
                        FirebaseDatabase.getInstance().getReference().child("temp").setValue("");
                        addMode();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(getContext().getApplicationContext(), "取消設定", Toast.LENGTH_SHORT).show();
                        FirebaseDatabase.getInstance().getReference().child("temp").setValue("");
                        addMode();
                    }
                })
                .show();
    }

    void addMode() {
        if (addButton.getText().equals("新增")) {
            addButton.setText("等待信號");
            sendButton.setEnabled(false);
            editButton.setEnabled(false);
            alarmButton.setEnabled(false);
            if (!isBluetoothConnected)
                setMode.setValue("recv");
            else
                btRecv = true;
            Toast.makeText(getContext().getApplicationContext(), "再按一下取消新增", Toast.LENGTH_SHORT).show();
        } else {
            addButton.setText("新增");
            sendButton.setEnabled(true);
            editButton.setEnabled(true);
            alarmButton.setEnabled(true);
            if (!isBluetoothConnected)
                setMode.setValue("");
            else
                btRecv = false;
        }
    }

    class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(getContext(), "藍牙連接中", "請稍後...");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (bluetoothSocket == null) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice("98:D3:41:FD:36:C3");
                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(BTMODULEUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    bluetoothSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!ConnectSuccess) {
                btButton.setText("嘗試連線");
                Snackbar.make(getView(), "與裝置藍牙連線失敗", Snackbar.LENGTH_SHORT).show();
            } else {
                btButton.setText("已連線");
                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();
                connectedThread.write("test");
                Snackbar.make(getView(), "藍牙連線成功", Snackbar.LENGTH_SHORT).show();
            }
            isBluetoothConnected = ConnectSuccess;
            progress.dismiss();
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        //pause and wait for rest of data
                        bytes = mmInStream.available();
                        // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes);
                        // record how many bytes we actually read
                        bluetoothHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    void bluetoothDisconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                connectedThread.cancel();
            } catch (Exception ignored) {
            }
            isBluetoothConnected = false;
            bluetoothSocket = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothDisconnect();
    }
}
