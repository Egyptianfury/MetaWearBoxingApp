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
import android.database.Cursor;
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
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.processor.Average;
import com.mbientlab.metawear.processor.Comparison;
import com.mbientlab.metawear.processor.Pulse;
import com.mbientlab.metawear.processor.Rss;
import com.mbientlab.metawear.processor.Threshold;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    public ProgressBar spinner;
    private static final String TAG = "MetaWear GPIO Example";
    final String SWITCH_STREAM = "switch_stream";
    private final String MW_MAC_ADDRESS = "FD:8D:A0:20:CE:31";
    TextView dataHold;



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
    public static final String LOG_TAG1 = "ACCELL";
    public static final String ACCEL_DATA = "accel_data";
    public static final String LOG_TAGp = "PeakValue", PEAK_VALUE = "peak_value";

    public static final String LOG_TAG = "FreeFallDetection", FREE_FALL_KEY = "free_fall_key", NO_FREE_FALL_KEY = "no_free_fall_key";
    private Debug debugModule;
    private Logging loggingModule;

    public int freq = 0;
    public String stfreq;
    public float []A;
    public int count = 0;
    public float test;
    public String sttest;
    public int k = 0;
    public ArrayList arr = new ArrayList();
    public double amag;
    public double threshold;
    public boolean sw = true;

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

        dataHold = (TextView) findViewById(R.id.data);



        spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);

        findViewById(R.id.start_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mwBoard.isConnected()) {
                    Toast.makeText(MainActivity.this, "Sensor Is Not Connected, Please Connect to Start", Toast.LENGTH_LONG).show();
                    return;
                } else
                    accelModule.enableAxisSampling();
                accelModule.start();
                Toast.makeText(MainActivity.this, "Data is Being Logged", Toast.LENGTH_LONG).show();

            }
        });

        findViewById(R.id.stop_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mwBoard.isConnected()) {
                    Toast.makeText(MainActivity.this, "Sensor Is Not Connected, Cannot Disconnect", Toast.LENGTH_LONG).show();
                    return;
                }
                accelModule.stop();
                accelModule.disableAxisSampling();
                Toast.makeText(MainActivity.this, "Data Logging has been Stopped", Toast.LENGTH_LONG).show();
                for (int i = 0; i < arr.size(); i++) {
                    sttest = arr.get(i).toString();
                    Log.i(LOG_TAG1, sttest);

                    threshold = Double.parseDouble(sttest);

                    if (sw == false && threshold <= 10) {
                        sw = true;
                    }
                    if (threshold >= 14 && sw == true) {
                        freq = freq + 1;
                        sw = false;
                    }
                }
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                dateFormat.format(date);
                stfreq = String.valueOf(freq);

                boolean isInserted = ProfilePage.myDb.insertFreq(stfreq);
                if (isInserted)
                    Toast.makeText(MainActivity.this, "Data Saved", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this, "Data Not Saved", Toast.LENGTH_LONG).show();

                Toast.makeText(MainActivity.this, "You punched " + stfreq + " times.", Toast.LENGTH_LONG).show();
                freq = 0;
            }
    });



        findViewById(R.id.reset_but).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugModule.resetDevice();
            }
        });


        viewAllFreq();
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


                    accelModule.routeData().fromAxes()
                            .process(new Rss())
                            .process(new Average((byte) 4))
                            .process(new Threshold(0.5f, Threshold.OutputMode.BINARY))
                            .split()
                            .branch().process(new Comparison(Comparison.Operation.EQ, -1)).stream(FREE_FALL_KEY)
                            .branch().process(new Comparison(Comparison.Operation.EQ, 1)).stream(NO_FREE_FALL_KEY)
                            .end()
                            .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(FREE_FALL_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    Log.i(LOG_TAG, "Entered Free Fall");
                                }
                            });
                            result.subscribe(NO_FREE_FALL_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    Log.i(LOG_TAG, "Stopped Free Fall");
                                }
                            });
                        }

                    });


                    accelModule.routeData().fromAxes().stream(ACCEL_DATA)

                            //.process(new Rss()).
                            .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(ACCEL_DATA, new RouteManager.MessageHandler() {

                                @Override
                                public void process(Message message) {

                                    Log.i(LOG_TAG, message.getData(CartesianFloat.class).toString());
                                    amag =  9.8*(Math.sqrt(Math.pow(message.getData(CartesianFloat.class).x().doubleValue(), 2)+ Math.pow(message.getData(CartesianFloat.class).y().doubleValue(), 2) + Math.pow(message.getData(CartesianFloat.class).z().doubleValue(), 2)));
                                    arr.add(amag);


                                }
                            });
                        }
                    });

                 /*accelModule.routeData().fromAxes().stream(PEAK_VALUE)
                            .process(new Rss())
                            .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(PEAK_VALUE, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    Log.i(LOG_TAGp, message.getData().toString());
                                }
                            });

                        }

                    });*/

                    debugModule = mwBoard.getModule(Debug.class);
                    loggingModule= mwBoard.getModule(Logging.class);

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

    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public void viewAllFreq(){
        findViewById(R.id.db_freq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor res = ProfilePage.myDb.getAllFreq();
                if (res.getCount() == 0) {
                    showMessage("Error", "No User Data");
                    return;
                }
                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()) {
                    buffer.append("ID: " + res.getString(0) + "\n");
                    buffer.append("Frequency: " + res.getString(1) + "\n");
                }
                showMessage("User Data", buffer.toString());
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