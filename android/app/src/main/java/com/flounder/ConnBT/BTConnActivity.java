package com.flounder.ConnBT;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;


public class BTConnActivity extends Activity {

    private ListView mListView;
    private TextView mTVInfo;
    private TextView mTVStatus;
    private TextView mTVLog;
    private Menu mMenu;

    private ArrayAdapter<String> mArrayAdapter;
    private BluetoothAdapter mAdapter;

    private BTService mBTService;
    private Handler mHandler;

    private static final int REQ_ENABLE_BT = 1;

    private int mState;
    private static final int STATE_START_WATERING = 1;
    private static final int STATE_STOP_WATERING = 2;
    private static final int STATE_GET_HUMIDITY = 3;
    private static final int STATE_SET_HUMIDITY = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initView();
        checkBTState();
        setupHandler();
    }

    private void checkBTState() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, R.string.text_error_bt, Toast.LENGTH_LONG).show();
            finish();
        }
        if (!mAdapter.isEnabled()) {
            mTVInfo.setText(R.string.text_enable_bt);
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQ_ENABLE_BT);
        } else {
            setPairedDeviceList();
        }

    }

    private void setPairedDeviceList() {
        for (BluetoothDevice device : mAdapter.getBondedDevices()) {
            mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        }
    }

    private void setupHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BTService.STATE_NONE:
                            case BTService.STATE_LISTEN:
                                mTVInfo.setText(R.string.text_conn_bt);
                                mTVLog.setText("");
                                mTVStatus.setText("");
                                mListView.setVisibility(View.VISIBLE);
                                toggleOptions(false);
                                break;
                            case BTService.STATE_CONNECTING:
                                mTVInfo.setText(R.string.text_conn_bt);
                                mTVLog.setText("");
                                mTVStatus.setText("");
                                mListView.setVisibility(View.VISIBLE);
                                toggleOptions(false);
                                break;
                            case BTService.STATE_CONNECTED:
                                mListView.setVisibility(View.GONE);
                                toggleOptions(true);
                                break;
                            default:
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = Utils.byteArrayToHex(readBuf);
                        setSpanText(Constants.MESSAGE_READ);
                        mTVLog.append(readMessage + "\n");
                        setTVStatus(readMessage);
                        break;
                    case Constants.MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = Utils.byteArrayToHex(writeBuf);
                        setSpanText(Constants.MESSAGE_WRITE);
                        mTVLog.append(writeMessage + "\n");
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        mTVInfo.setText(getResources().getString(R.string.text_connected_device) +
                                msg.getData().getString(Constants.DEVICE_NAME) + "\n\n");
                        mTVLog.setText("");
                        toggleOptions(true);
                        break;
                    case Constants.MESSAGE_TOAST:
                        // Connection Error
                        mTVInfo.setText(msg.getData().getString(Constants.TOAST));
                        mTVLog.setText("");
                        toggleOptions(true);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void setTVStatus(String response) {
        switch (mState) {
            case STATE_SET_HUMIDITY:
                if (response.equals(Utils.byteArrayToHex(Protocol.RES_SET_HUMIDITY))) {
                    mTVStatus.setText(R.string.text_success_set_humidity);
                }
                break;
            case STATE_START_WATERING:
                if (response.equals(Utils.byteArrayToHex(Protocol.RES_START_WATERING))) {
                    mTVStatus.setText(R.string.text_success_start_watering);
                }
                break;
            case STATE_STOP_WATERING:
                if (response.equals(Utils.byteArrayToHex(Protocol.RES_STOP_WATERING))) {
                    mTVStatus.setText(R.string.text_success_stop_watering);
                }
                break;
            case STATE_GET_HUMIDITY:
                mTVStatus.setText(getResources().getString(R.string.text_success_get_humidity)
                        + Protocol.getHumidity(response));
                break;
        }

    }

    private void setSpanText(int flag) {
        String tmp = "";
        int color = 0;
        switch (flag) {
            case Constants.MESSAGE_WRITE:
                tmp = " (Send)\n";
                color = Color.LTGRAY;
                break;
            case Constants.MESSAGE_READ:
                tmp = " (Receive)\n";
                color = Color.GREEN;
                break;
            default:
                break;
        }
        String dateStr = ">>> " + DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()) + tmp;
        SpannableStringBuilder word = new SpannableStringBuilder(dateStr);
        word.setSpan(new ForegroundColorSpan(color), 0, dateStr.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mTVLog.append(word);
    }

    private void initView() {
        mTVInfo = (TextView) findViewById(R.id.text_info);
        mTVInfo.setText(R.string.text_conn_bt + "\n");
        mTVLog = (TextView) findViewById(R.id.text_log);
        mTVLog.setMovementMethod(new ScrollingMovementMethod());
        mTVStatus = (TextView) findViewById(R.id.text_status);
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mListView = (ListView) findViewById(R.id.listview);
        mListView.setAdapter(mArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                BluetoothDevice device = mAdapter.getRemoteDevice(address);
                mBTService.connect(device);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null && mAdapter.isEnabled()) {
            if (mBTService != null) {
                if (mBTService.getState() == BTService.STATE_NONE) {
                    mBTService.start();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAdapter != null && mAdapter.isEnabled()) {
            if (mBTService == null) {
                mBTService = new BTService(this, mHandler);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBTService != null) {
            mBTService.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            // BlueTooth cannot be opened or BT not available
            Toast.makeText(BTConnActivity.this, R.string.text_enable_bt, Toast.LENGTH_LONG).show();
            finish();
        } else if (resultCode == RESULT_OK && requestCode == REQ_ENABLE_BT) {
            setPairedDeviceList();
            if (mBTService == null) {
                mBTService = new BTService(this, mHandler);
            }
            if (mBTService.getState() == BTService.STATE_NONE) {
                mBTService.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        mMenu = menu;
        toggleOptions(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_set_humidity:
                mState = STATE_SET_HUMIDITY;
                handleSetHumidity();
                mTVStatus.setText(R.string.set_humidity);
                break;
            case R.id.action_get_humidity:
                mState = STATE_GET_HUMIDITY;
                mTVStatus.setText(R.string.get_humidity);
                mBTService.write(Protocol.REQ_GET_HUMIDITY);
                break;
            case R.id.action_start_watering:
                mState = STATE_START_WATERING;
                mTVStatus.setText(R.string.start_watering);
                mBTService.write(Protocol.REQ_START_WATERING);
                break;
            case R.id.action_stop_humidity:
                mState = STATE_STOP_WATERING;
                mTVStatus.setText(R.string.stop_watering);
                mBTService.write(Protocol.REQ_STOP_WATERING);
                break;
            case R.id.action_about:
                Toast.makeText(this, R.string.text_about,Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSetHumidity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog, null);
        final EditText etHigh = (EditText) view.findViewById(R.id.et_high_humidity);
        final EditText etLow = (EditText) view.findViewById(R.id.et_low_humidity);
        builder.setTitle(R.string.dialog_title_set_humidity)
               .setView(view)
               .setCancelable(false)
               .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       if (!TextUtils.isEmpty(etHigh.getText()) && !TextUtils.isEmpty(etLow.getText())) {
                           int high = Integer.parseInt(etHigh.getText().toString());
                           int low = Integer.parseInt(etLow.getText().toString());
                           if (high <= low || (high >= 100 || high <= 0) || (low >= 100 || low <= 0)) {
                               Toast.makeText(BTConnActivity.this,
                                       R.string.dialog_invalid_value, Toast.LENGTH_SHORT).show();
                           } else {
                               byte[] tmp = {0x00, (byte) high, 0x00, (byte) low, (byte) 0xFF};
                               mBTService.write(Utils.combineByteArray(Protocol.REQ_SET_HUMIDITY, tmp));
                           }
                       }
                   }
               })
               .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                   }
               });
        builder.show();
    }

    private void toggleOption(int itemId, boolean flag) {
        if (mMenu != null) {
            MenuItem item = mMenu.findItem(itemId);
            item.setVisible(flag);
        }
    }

    private void toggleOptions(boolean flag) {
        toggleOption(R.id.action_get_humidity, flag);
        toggleOption(R.id.action_set_humidity, flag);
        toggleOption(R.id.action_start_watering, flag);
        toggleOption(R.id.action_stop_humidity, flag);
    }
}