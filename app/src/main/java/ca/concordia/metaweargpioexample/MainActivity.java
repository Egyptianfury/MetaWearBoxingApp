/*
 * MetaWearGPIOExample
 *
 * 2015 T. Trezise
 *
 * 2015-07: using MetaWear API v2.0.0 Beta 1
 * https://github.com/mbientlab/Metawear-AndroidAPI/releases/tag/2.0.0-beta.01
 *
 * 2015-07-27: using API v2.0.0 Beta 4
 * https://github.com/mbientlab/Metawear-AndroidAPI/releases/tag/2.0.0-beta.04
 *
 * Use of the MetaWear API is subject to their terms:
 * www.mbientlab.com/terms
 *
 */
package ca.concordia.metaweargpioexample;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.MetaWearBoard.DeviceInformation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Gpio.AnalogReadMode;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Timer;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    public ProgressBar spinner;
    private static final String TAG = "MetaWear GPIO Example";
    final String SWITCH_STREAM = "switch_stream";
    private final String MW_MAC_ADDRESS = "FD:8D:A0:20:CE:31";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.bt_status)).setText("Disabled");
                                ((TextView) findViewById(R.id.bt_status)).setTextColor(Color.rgb(200, 0, 0));
                            }
                        });
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.bt_status)).setText("Turning off...");
                            }
                        });

                        break;
                    case BluetoothAdapter.STATE_ON:
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.bt_status)).setText("Enabled");
                                ((TextView) findViewById(R.id.bt_status)).setTextColor(Color.rgb(0, 200, 0));
                            }
                        });

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.bt_status)).setText("Turning on...");
                            }
                        });
                        break;
                }
            }
        }
    };

    public MetaWearBoard mwBoard;
    public MetaWearBleService.LocalBinder serviceBinder;
    public Accelerometer accelModule;
    public static final String LOG_TAG = "ACCELL";
    public static final String ACCEL_DATA = "accel_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProfilePage.myDb = new DatabaseHelper(this);


        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);


        Button profilePage = (Button) findViewById(R.id.profPage);
        profilePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfilePageDisplay.class);
                startActivity(intent);
            }
        });

        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);

        findViewById(R.id.start_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mwBoard.isConnected()) {
                    Toast.makeText(MainActivity.this, "Sensor Is Not Connected, Please Connect to Start", Toast.LENGTH_LONG).show();
                    return;
                }
                else
                    accelModule.enableAxisSampling();
                    accelModule.start();
                    Toast.makeText(MainActivity.this, "Data is Being Logged", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.stop_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mwBoard.isConnected()){
                    Toast.makeText(MainActivity.this, "Sensor Is Not Connected, Cannot Disconnect", Toast.LENGTH_LONG).show();
                    return;
                }
                accelModule.stop();
                accelModule.disableAxisSampling();
                Toast.makeText(MainActivity.this, "Data Logging has been Stopped", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        getApplicationContext().unbindService(this);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                if (!mwBoard.isConnected()) {
                    connectBoard();
                }
                return true;
            case R.id.action_disconnect:
                if (mwBoard.isConnected()) {
                    mwBoard.disconnect();
                }
                return true;
            case R.id.action_reset:
                if (mwBoard.isConnected()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Reset MetaWear PCB")
                            .setMessage("Sure you want to reset?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        mwBoard.getModule(Debug.class).resetDevice();
                                        Toast.makeText(MainActivity.this, "MetaWear Resetting", Toast.LENGTH_LONG).show();
                                    } catch (UnsupportedModuleException ignored) {
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return true;
                } else {
                    Toast.makeText(MainActivity.this, "Nothing connected", Toast.LENGTH_LONG).show();
                    return true;
                }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (MetaWearBleService.LocalBinder) service;
        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
        ((TextView) findViewById(R.id.bt_status)).setText(btManager.getAdapter().isEnabled() ? "Enabled" : "Disabled");
        ((TextView) findViewById(R.id.bt_status)).setTextColor(btManager.getAdapter().isEnabled() ? Color.rgb(0, 200, 0) : Color.rgb(200, 0, 0));
        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    public void connectBoard() {
        spinner.setVisibility(View.VISIBLE);
        mwBoard.setConnectionStateHandler(new ConnectionStateHandler() {
            @Override
            public void connected() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                        ((TextView) findViewById(R.id.MACaddr)).setText("Connected to " + MW_MAC_ADDRESS);
                        spinner.setVisibility(View.GONE);

                    }
                });
                try {
                    accelModule = mwBoard.getModule(Accelerometer.class);
                    accelModule.setOutputDataRate(5f);
                    accelModule.routeData().fromAxes().stream(ACCEL_DATA).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(ACCEL_DATA, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                   Log.i(LOG_TAG, message.getData(CartesianFloat.class).toString());
                                }
                            });
                        }
                    });
                } catch (UnsupportedModuleException e) {
                    e.printStackTrace();
                }

                updateDeviceInfo();
                updateBatt();
            }

            @Override
            public void disconnected() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connection Lost", Toast.LENGTH_LONG).show();
                    }
                });
                clearVals();
            }

            @Override
            public void failure(int status, Throwable error) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error Connecting", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Error connecting", error);
            }
        });
        mwBoard.connect();
    }

    void updateDeviceInfo() {
        mwBoard.readDeviceInformation().onComplete(new AsyncOperation.CompletionHandler<DeviceInformation>() {
            @Override
            public void success(final DeviceInformation result) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.manuf)).setText(result.manufacturer());
                        ((TextView) findViewById(R.id.serial)).setText(result.serialNumber());
                        ((TextView) findViewById(R.id.firmware)).setText(result.firmwareRevision());
                        ((TextView) findViewById(R.id.hardware)).setText(result.hardwareRevision());
                    }
                });
            }

            @Override
            public void failure(Throwable error) {
                Log.e(TAG, "Error reading Device Info", error);
            }
        });
    }

    void updateBatt() {
        mwBoard.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
            @Override
            public void success(final Byte result) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.batt)).setText(result.toString());
                    }
                });
            }

            @Override
            public void failure(Throwable error) {
                Log.e(TAG, "Error reading Battery level", error);
            }
        });
    }

    public void clearVals() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.MACaddr)).setText("");
                ((TextView) findViewById(R.id.manuf)).setText("");
                ((TextView) findViewById(R.id.firmware)).setText("");
                ((TextView) findViewById(R.id.serial)).setText("");
                ((TextView) findViewById(R.id.hardware)).setText("");
                ((TextView) findViewById(R.id.batt)).setText("");
                ((TextView) findViewById(R.id.rssi)).setText("");
                ((TextView) findViewById(R.id.analogVal)).setText("");
                ((TextView) findViewById(R.id.switchState)).setText("");
            }
        });
    }
}