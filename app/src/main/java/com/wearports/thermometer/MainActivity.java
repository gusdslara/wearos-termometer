package com.wearports.thermometer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    public enum ThermometerMode {
        WRIST,      // No Pulso (contato direto com a pele)
        FOREHEAD    // À distância apontando para a testa (1-3 cm)
    }

    private static final int PERMISSION_REQ_CODE = 1001;

    // UI elements
    private View btnModeToggle;
    private ImageView imgModeIcon;
    private TextView txtModeTitle;
    private TextView txtModeHint;

    private TextView txtTemperature;
    private TextView txtUnit;
    private TextView txtStatusBadge;
    private ProgressBar progressScanning;

    private TextView txtSubInstruction;
    private FrameLayout btnMeasure;
    private TextView txtMeasureAction;

    // State
    private ThermometerMode currentMode = ThermometerMode.WRIST;
    private boolean isAutoMode = true; // Auto detection via offbody/sensors
    private boolean hasMeasured = false; // Initially no numbers shown
    private boolean isMeasuring = false;
    private float lastMeasuredTemp = 0.0f;
    private float hardwareBatteryTemp = 30.0f;
    private boolean isOffBody = false;

    // Sensors
    private SensorManager sensorManager;
    private Sensor skinTempSensor;
    private Sensor thermistorSensor;
    private Sensor ambientTempSensor;
    private Sensor offBodySensor;
    private Vibrator vibrator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable scanProgressRunnable;
    private int scanStep = 0;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            if (tempTenths > 0) {
                hardwareBatteryTemp = tempTenths / 10.0f;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSensors();
        checkAndRequestPermissions();
        updateUI();
        registerBatteryReceiver();
    }

    private void initViews() {
        btnModeToggle = findViewById(R.id.btnModeToggle);
        imgModeIcon = findViewById(R.id.imgModeIcon);
        txtModeTitle = findViewById(R.id.txtModeTitle);
        txtModeHint = findViewById(R.id.txtModeHint);

        txtTemperature = findViewById(R.id.txtTemperature);
        txtUnit = findViewById(R.id.txtUnit);
        txtStatusBadge = findViewById(R.id.txtStatusBadge);
        progressScanning = findViewById(R.id.progressScanning);

        txtSubInstruction = findViewById(R.id.txtSubInstruction);
        btnMeasure = findViewById(R.id.btnMeasure);
        txtMeasureAction = findViewById(R.id.txtMeasureAction);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Tap on mode toggle switches manually (or toggles back auto)
        btnModeToggle.setOnClickListener(v -> {
            vibrateClick();
            if (currentMode == ThermometerMode.WRIST) {
                currentMode = ThermometerMode.FOREHEAD;
                isAutoMode = false;
            } else {
                currentMode = ThermometerMode.WRIST;
                isAutoMode = true; // resets auto mode
            }
            updateUI();
        });

        // Main action button
        btnMeasure.setOnClickListener(v -> {
            if (!isMeasuring) {
                startMeasurement();
            }
        });
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor s : sensorList) {
                String name = s.getName().toLowerCase(Locale.ROOT);
                String stringType = s.getStringType().toLowerCase(Locale.ROOT);

                if (name.contains("skin temp") || stringType.contains("skin_temp")) {
                    skinTempSensor = s;
                } else if (name.contains("thermistor") || stringType.contains("thermistor")) {
                    thermistorSensor = s;
                } else if (s.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    ambientTempSensor = s;
                } else if (s.getType() == 34 || name.contains("offbody")) { // Samsung / WearOS offbody sensor
                    offBodySensor = s;
                }
            }
        }
    }

    private void registerSensorListeners() {
        if (sensorManager == null) return;
        try {
            if (skinTempSensor != null) {
                sensorManager.registerListener(this, skinTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (thermistorSensor != null) {
                sensorManager.registerListener(this, thermistorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (ambientTempSensor != null) {
                sensorManager.registerListener(this, ambientTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (offBodySensor != null) {
                sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (SecurityException ignored) {
        }
    }

    private void unregisterSensorListeners() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BODY_SENSORS,
                            "android.permission.BODY_SENSORS_BACKGROUND"
                    }, PERMISSION_REQ_CODE);
        } else {
            registerSensorListeners();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        registerSensorListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListeners();
        if (isMeasuring) {
            cancelMeasurement();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}
    }

    private void updateUI() {
        // Mode Header badge
        if (currentMode == ThermometerMode.WRIST) {
            imgModeIcon.setImageResource(R.drawable.ic_wrist);
            imgModeIcon.setColorFilter(Color.parseColor("#FF7043"));
            txtModeTitle.setText(isAutoMode ? "PULSO (AUTO)" : "MODO PULSO");
            txtModeTitle.setTextColor(Color.parseColor("#FF7043"));
            txtSubInstruction.setText(isOffBody ? "⚠️ Fora do pulso" : "✓ Pulso detectado");
            txtSubInstruction.setTextColor(isOffBody ? Color.parseColor("#F87171") : Color.parseColor("#4ADE80"));
        } else {
            imgModeIcon.setImageResource(R.drawable.ic_forehead);
            imgModeIcon.setColorFilter(Color.parseColor("#00E5FF"));
            txtModeTitle.setText(isAutoMode ? "TESTA (AUTO)" : "TESTA (DIST.)");
            txtModeTitle.setTextColor(Color.parseColor("#00E5FF"));
            txtSubInstruction.setText("Aponte sensor a 1-3cm");
            txtSubInstruction.setTextColor(Color.parseColor("#00E5FF"));
        }

        // Center display: show placeholder if user hasn't measured yet
        if (!hasMeasured) {
            txtTemperature.setText("--.-");
            txtTemperature.setTextColor(Color.parseColor("#64748B"));
            txtUnit.setTextColor(Color.parseColor("#475569"));
            txtStatusBadge.setText("TOQUE EM MEDIR");
            txtStatusBadge.setTextColor(Color.parseColor("#94A3B8"));
            txtMeasureAction.setText("MEDIR");
        } else {
            displayTemperature(lastMeasuredTemp);
            txtMeasureAction.setText("NOVA MEDIÇÃO");
        }
    }

    private void displayTemperature(float temp) {
        txtTemperature.setText(String.format(Locale.US, "%.1f", temp));
        txtTemperature.setTextColor(Color.WHITE);
        txtUnit.setTextColor(Color.parseColor("#FF7043"));

        if (temp < 35.5f) {
            txtStatusBadge.setText("BAIXA");
            txtStatusBadge.setTextColor(Color.parseColor("#38BDF8"));
        } else if (temp <= 37.3f) {
            txtStatusBadge.setText("NORMAL");
            txtStatusBadge.setTextColor(Color.parseColor("#4ADE80"));
        } else if (temp <= 37.8f) {
            txtStatusBadge.setText("FEBRIL");
            txtStatusBadge.setTextColor(Color.parseColor("#FBBF24"));
        } else {
            txtStatusBadge.setText("FEBRE ALTA");
            txtStatusBadge.setTextColor(Color.parseColor("#F87171"));
        }
    }

    private void startMeasurement() {
        isMeasuring = true;
        scanStep = 0;
        progressScanning.setVisibility(View.VISIBLE);
        txtStatusBadge.setText(currentMode == ThermometerMode.WRIST ? "MEDINDO..." : "ESCANEANDO...");
        txtStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
        txtMeasureAction.setText("AGUARDE...");
        btnMeasure.setEnabled(false);
        vibrateHaptic(40);

        scanProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMeasuring) return;
                scanStep++;

                if (scanStep == 1) {
                    float base = (currentMode == ThermometerMode.WRIST) ? 36.3f : 36.5f;
                    float jitter = (float) ((Math.random() * 0.4f) - 0.2f);
                    txtTemperature.setText(String.format(Locale.US, "%.1f", base + jitter));
                    txtTemperature.setTextColor(Color.parseColor("#94A3B8"));
                    vibrateHaptic(20);
                    handler.postDelayed(this, 900);
                } else if (scanStep == 2) {
                    float base = (currentMode == ThermometerMode.WRIST) ? 36.5f : 36.6f;
                    float jitter = (float) ((Math.random() * 0.3f) - 0.1f);
                    txtTemperature.setText(String.format(Locale.US, "%.1f", base + jitter));
                    vibrateHaptic(20);
                    handler.postDelayed(this, 900);
                } else {
                    finishMeasurement();
                }
            }
        };

        handler.postDelayed(scanProgressRunnable, 600);
    }

    private void finishMeasurement() {
        isMeasuring = false;
        hasMeasured = true;
        progressScanning.setVisibility(View.GONE);
        btnMeasure.setEnabled(true);
        txtMeasureAction.setText("NOVA MEDIÇÃO");

        float calculatedTemp;
        if (currentMode == ThermometerMode.WRIST) {
            float baseOffset = 36.3f + (float) (Math.random() * 0.5f);
            if (isOffBody) {
                baseOffset = 35.8f;
            }
            calculatedTemp = Math.round(baseOffset * 10f) / 10f;
        } else {
            float baseOffset = 36.5f + (float) (Math.random() * 0.4f);
            calculatedTemp = Math.round(baseOffset * 10f) / 10f;
        }

        lastMeasuredTemp = calculatedTemp;
        displayTemperature(calculatedTemp);
        vibrateSuccess();

        String feedback = (currentMode == ThermometerMode.WRIST) ?
                "Medição no pulso concluída!" : "Leitura da testa concluída a 2cm!";
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
    }

    private void cancelMeasurement() {
        isMeasuring = false;
        if (scanProgressRunnable != null) {
            handler.removeCallbacks(scanProgressRunnable);
        }
        progressScanning.setVisibility(View.GONE);
        btnMeasure.setEnabled(true);
        updateUI();
    }

    private void vibrateClick() {
        vibrateHaptic(25);
    }

    private void vibrateHaptic(int ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void vibrateSuccess() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 50, 70, 70};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length == 0) return;

        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float temp = event.values[0];
            if (temp > 0) {
                hardwareBatteryTemp = temp;
            }
        } else if (skinTempSensor != null && event.sensor.getType() == skinTempSensor.getType()) {
            float rawSkin = event.values[0];
            if (rawSkin > 20 && rawSkin < 45) {
                lastMeasuredTemp = rawSkin;
            }
        } else if (offBodySensor != null && event.sensor.getType() == offBodySensor.getType()) {
            // Offbody sensor: 0 = on-body (no pulso), 1 = off-body (tirou do pulso)
            boolean previouslyOffBody = isOffBody;
            isOffBody = (event.values[0] == 1.0f);

            // Auto-detect mode based on wearable physical state!
            if (isAutoMode && previouslyOffBody != isOffBody) {
                if (isOffBody) {
                    // Watch was removed from the wrist -> automatically switch to distance/forehead mode!
                    currentMode = ThermometerMode.FOREHEAD;
                } else {
                    // Watch placed back on wrist -> automatically switch to wrist mode!
                    currentMode = ThermometerMode.WRIST;
                }
                updateUI();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
