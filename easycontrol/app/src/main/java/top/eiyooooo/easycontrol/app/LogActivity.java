package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import top.eiyooooo.easycontrol.app.databinding.ActivityLogBinding;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.helper.DeviceListAdapter;
import top.eiyooooo.easycontrol.app.helper.L;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

public class LogActivity extends Activity {
  private ActivityLogBinding logActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    logActivity = ActivityLogBinding.inflate(this.getLayoutInflater());
    setContentView(logActivity.getRoot());
    ArrayAdapter<String> devices = new ArrayAdapter<>(this, R.layout.item_spinner_item);
    devices.add(getString(R.string.log_other_devices));
    for (Device device : DeviceListAdapter.devicesList) {
      devices.add(device.name);
    }
    logActivity.backButton.setOnClickListener(v -> finish());
    logActivity.logDevice.setAdapter(devices);
    logActivity.logDevice.setSelection(0);
    logActivity.logText.setMovementMethod(new ScrollingMovementMethod());
    logActivity.logRefresh.setOnClickListener(v -> refreshLogText());
    logActivity.logCopy.setOnClickListener(v -> copyCurrentLog());
    logActivity.logClear.setOnClickListener(v -> {
      String uuid = getSelectedDeviceUuid();
      if (uuid == null) L.clearLogs();
      else L.clearLogs(uuid);
      refreshLogText();
    });
    logActivity.logDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        refreshLogText();
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private void refreshLogText() {
    String uuid = getSelectedDeviceUuid();
    logActivity.logText.setText(uuid == null ? L.getLogs() : L.getLogs(uuid));
  }

  private void copyCurrentLog() {
    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    clipboardManager.setPrimaryClip(ClipData.newPlainText("easycontrol_log", logActivity.logText.getText()));
    PublicTools.logToast(getString(R.string.log_copied));
  }

  private String getSelectedDeviceUuid() {
    if (logActivity.logDevice.getSelectedItemPosition() == 0) return null;
    String selectedDevice = logActivity.logDevice.getSelectedItem().toString();
    for (Device device : DeviceListAdapter.devicesList) {
      if (device.name.equals(selectedDevice)) return device.uuid;
    }
    return null;
  }
}
