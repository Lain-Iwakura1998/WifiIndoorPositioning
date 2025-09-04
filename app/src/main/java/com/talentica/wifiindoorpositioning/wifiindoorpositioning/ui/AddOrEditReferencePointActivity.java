package com.talentica.wifiindoorpositioning.wifiindoorpositioning.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.talentica.wifiindoorpositioning.wifiindoorpositioning.R;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.adapter.ReferenceReadingsAdapter;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.AccessPoint;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.IndoorProject;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.ReferencePoint;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.utils.AppContants;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddOrEditReferencePointActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "AddOrEditReferencePointActivity";
    private String projectId;

    private RecyclerView rvPoints;
    private LinearLayoutManager layoutManager;
    private EditText etRpName, etRpX, etRpY;
    private Button bnRpSave;

    private ReferenceReadingsAdapter readingsAdapter = new ReferenceReadingsAdapter();
    private List<AccessPoint> apsWithReading = new ArrayList<>();
    private Map<String, List<Integer>> readings = new HashMap<>();
    private Map<String, AccessPoint> aps = new HashMap<>();

    private AvailableAPsReceiver receiverWifi;

    private boolean wifiWasEnabled;
    private WifiManager mainWifi;
    private final Handler handler = new Handler();
    private boolean isCaliberating = false;
    private int readingsCount = 0;
    private boolean isEdit = false;
    private String rpId;
    private ReferencePoint referencePointFromDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reference_point);

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "Reference point not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (getIntent().getStringExtra("rpId") != null) {
            isEdit = true;
            rpId = getIntent().getStringExtra("rpId");
        }
        initUI();

        if (isEdit) {
            // 从内存中查找 ReferencePoint
            for (IndoorProject p : NewProjectActivity.projectList) {
                if (p.getId().equals(projectId)) {
                    for (ReferencePoint rp : p.getRps()) {
                        if (rp.getId().equals(rpId)) {
                            referencePointFromDB = rp;
                            break;
                        }
                    }
                }
            }
            if (referencePointFromDB == null) {
                Toast.makeText(this, "Reference point not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            for (AccessPoint ap : referencePointFromDB.getReadings()) {
                readingsAdapter.addAP(ap);
            }
            readingsAdapter.notifyDataSetChanged();
            etRpName.setText(referencePointFromDB.getName());
            etRpX.setText(String.valueOf(referencePointFromDB.getX()));
            etRpY.setText(String.valueOf(referencePointFromDB.getY()));
        } else {
            mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            receiverWifi = new AvailableAPsReceiver();
            wifiWasEnabled = mainWifi.isWifiEnabled();

            IndoorProject project = null;
            for (IndoorProject p : NewProjectActivity.projectList) {
                if (p.getId().equals(projectId)) {
                    project = p;
                    break;
                }
            }
            if (project != null) {
                for (AccessPoint accessPoint : project.getAps()) {
                    aps.put(accessPoint.getMac_address(), accessPoint);
                }
            }
            if (aps.isEmpty()) {
                Toast.makeText(this, "No Access Points Found", Toast.LENGTH_SHORT).show();
            }
            if (!Utils.isLocationEnabled(this)) {
                Toast.makeText(this, "Please turn on the location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        if (!isEdit) {
            registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            Log.v(TAG, "caliberationStarted");
            if (!isCaliberating) {
                isCaliberating = true;
                refresh();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (!isEdit) {
            unregisterReceiver(receiverWifi);
            isCaliberating = false;
        }
        super.onPause();
    }

    public void refresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainWifi.startScan();
                if (readingsCount < AppContants.READINGS_BATCH) {
                    refresh();
                } else {
                    caliberationCompleted();
                }
            }
        }, AppContants.FETCH_INTERVAL);
    }

    private void caliberationCompleted() {
        isCaliberating = false;
        Log.v(TAG, "caliberationCompleted");
        for (Map.Entry<String, List<Integer>> entry : readings.entrySet()) {
            List<Integer> readingsOfAMac = entry.getValue();
            Double mean = calculateMeanValue(readingsOfAMac);
            AccessPoint accessPoint = aps.get(entry.getKey());
            AccessPoint updatedPoint = new AccessPoint(accessPoint);
            updatedPoint.setMeanRss(mean);
            apsWithReading.add(updatedPoint);
        }
        readingsAdapter.setReadings(apsWithReading);
        readingsAdapter.notifyDataSetChanged();
        bnRpSave.setEnabled(true);
        bnRpSave.setText("Save");
    }

    private Double calculateMeanValue(List<Integer> readings) {
        if (readings.isEmpty()) {
            return 0.0d;
        }
        int sum = 0;
        for (Integer integer : readings) {
            sum += integer;
        }
        return (double) sum / readings.size();
    }

    private void initUI() {
        layoutManager = new LinearLayoutManager(this);
        rvPoints = findViewById(R.id.rv_points);
        rvPoints.setLayoutManager(layoutManager);
        rvPoints.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvPoints.setAdapter(readingsAdapter);

        bnRpSave = findViewById(R.id.bn_rp_save);
        bnRpSave.setOnClickListener(this);

        if (!isEdit) {
            bnRpSave.setEnabled(false);
            bnRpSave.setText("Caliberating...");
        } else {
            bnRpSave.setEnabled(true);
            bnRpSave.setText("Save");
        }

        etRpName = findViewById(R.id.et_rp_name);
        etRpX = findViewById(R.id.et_rp_x);
        etRpY = findViewById(R.id.et_rp_y);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == bnRpSave.getId() && !isEdit) {
            ReferencePoint referencePoint = new ReferencePoint();
            referencePoint = setValues(referencePoint);
            referencePoint.setCreatedAt(Calendar.getInstance().getTime());
            referencePoint.setDescription("");
            referencePoint.setReadings(new ArrayList<>(apsWithReading));
            referencePoint.setId(UUID.randomUUID().toString());

            for (IndoorProject p : NewProjectActivity.projectList) {
                if (p.getId().equals(projectId)) {
                    p.getRps().add(referencePoint);
                    break;
                }
            }
            Toast.makeText(this, "Reference Point Added", Toast.LENGTH_SHORT).show();
            finish();
        } else if (view.getId() == bnRpSave.getId() && isEdit) {
            referencePointFromDB = setValues(referencePointFromDB);
            Toast.makeText(this, "Reference Point Updated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private ReferencePoint setValues(ReferencePoint referencePoint) {
        String x = etRpX.getText().toString();
        String y = etRpY.getText().toString();
        referencePoint.setName(etRpName.getText().toString());
        if (!TextUtils.isEmpty(x)) {
            referencePoint.setX(Double.parseDouble(x));
        }
        if (!TextUtils.isEmpty(y)) {
            referencePoint.setY(Double.parseDouble(y));
        }
        referencePoint.setLocId(referencePoint.getX
