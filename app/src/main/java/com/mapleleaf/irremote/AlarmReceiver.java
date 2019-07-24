package com.mapleleaf.irremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Objects;

public class AlarmReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bData = intent.getExtras();
        DatabaseReference firebase = FirebaseDatabase .getInstance() .getReference();
        CharSequence date = DateFormat.format("yyyy/MM/dd", Calendar .getInstance() .getTime());
        CharSequence time = DateFormat.format("kk:mm:ss", Calendar .getInstance() .getTime());
        firebase.child("mode").setValue("trans");
        firebase.child("trans").setValue(Objects.requireNonNull(bData).getString("trans"));
        HistoryFragment.historyAdd("history," + bData .getString("name") + "," + date + "/" + time + ",自動控制", "alarm");
    }
}
