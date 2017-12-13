package com.ut.lulyfan.exrobot.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.util.CustomerDBUtil;
import com.ut.lulyfan.exrobot.util.ExcelUtil;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class SettingActivity extends Activity {

    Button bt_sure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bt_sure = (Button) findViewById(R.id.sure);
        getFragmentManager().beginTransaction()
                            .add(R.id.fragmentContainer, new SettingsFragment())
                            .commit();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.sure)
        {
            Intent intent = new Intent(this, ExActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.importAddress) {
            showFileChooser();
        }
    }

    private static final int FILE_SELECT_CODE = 0;
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult( Intent.createChooser(intent, "请选择Excel地址库文件"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "请先安装一个文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    private static final String TAG = "ChooseFile";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {

                    bt_sure.setEnabled(false);

                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    List<Customer> customers = null;
                    try {
                        String  path = getPath(this, uri);
                        Log.d(TAG, "File Path: " + path);
                        customers = ExcelUtil.parse(path);
                        CustomerDBUtil customerDBUtil = CustomerDBUtil.getInstance();
                        customerDBUtil.deleteAll();

                        for (Customer customer : customers)
                            customerDBUtil.write(customer);
                        Toast.makeText(this, "数据导入完成", Toast.LENGTH_SHORT).show();

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (InvalidFormatException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Excel地址库坐标数据错误："+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    bt_sure.setEnabled(true);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

        public static final String KEY_FLOOR = "floor";
        public static final String KEY_INIT_POSITION = "initPosition";
        public static final String KEY_EX_POSITION = "exPosition";
        public static final String KEY_SN = "sn";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();

            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            findPreference(KEY_SN).setSummary(sp.getString(KEY_SN, "未知"));
            findPreference(KEY_FLOOR).setSummary(sp.getString(KEY_FLOOR, "1"));
            findPreference(KEY_INIT_POSITION).setSummary(sp.getString(KEY_INIT_POSITION, ""));
            findPreference(KEY_EX_POSITION).setSummary(sp.getString(KEY_EX_POSITION, ""));
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (key.equals(KEY_SN)) {

                Preference snSP = findPreference(key);
                String sn = sharedPreferences.getString(key, "未知");
                snSP.setSummary(sn);

            }   else if (key.equals(KEY_FLOOR)) {

                Preference floorSP = findPreference(key);
                String floor = sharedPreferences.getString(key, "1");
                floorSP.setSummary(floor);

            }  else if (key.equals(KEY_INIT_POSITION)) {

                Preference initPositionSP = findPreference(key);
                String initPosition = sharedPreferences.getString(key, "");
                initPositionSP.setSummary(initPosition);

            } else if (key.equals(KEY_EX_POSITION)) {

                Preference exPositionSP = findPreference(key);
                String exPosition = sharedPreferences.getString(key, "");
                exPositionSP.setSummary(exPosition);

            }
        }
    }
}
