package com.mapleleaf.irremote;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class AlarmFragment extends Fragment {

    View view;
    static RecyclerView alarmRecyclerView;
    static AlarmFragment.AlarmAdapter alarmAdapter;
    static ArrayList<String> alarm = new ArrayList<>();
    ImageButton addAlarmButton;
    String repeat = "";

    public AlarmFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_alarm, container, false);
        for (LinkedHashMap.Entry<String, ?> entry : Prefs.getAll().entrySet()) {
            if (entry.getKey().contains("alarm,")) {
                alarm.add(entry.getKey().substring(6));
            }
        }
        // 連結元件
        alarmRecyclerView = view.findViewById(R.id.alarmRecyclerView);
        // 設置RecyclerView為列表型態
        alarmRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 設置格線
        alarmRecyclerView.addItemDecoration(new DividerItemDecoration(Objects.requireNonNull(getContext()), DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        alarmAdapter = new AlarmFragment.AlarmAdapter(alarm);
        // 設置adapter給recycler_view
        alarmRecyclerView.setAdapter(alarmAdapter);
        addAlarmButton = view.findViewById(R.id.addAlarmImageButton);
        /*addAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                @SuppressLint("InflateParams") final View alarmInfoView = inflater.inflate(R.layout.alarm_edit_dialog, null);
                Spinner spinner = alarmInfoView.findViewById(R.id.chooseSpinner);
                spinner.setAdapter(RemoteFragment.nameAdapter);
                EditText name = alarmInfoView.findViewById(R.id.editNameText);
                Button pickupTimeButton = alarmInfoView.findViewById(R.id.pickupTimeButton);
                pickupTimeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                Switch repeatSwitch = alarmInfoView.findViewById(R.id.alarmSwitch);
                repeatSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        @SuppressLint("InflateParams") final View setRepeatView = inflater.inflate(R.layout.alarm_set_repeat_dialog, null);
                        final CheckBox[] weekday = new CheckBox[]{setRepeatView.findViewById(R.id.monday),
                                setRepeatView.findViewById(R.id.tuesday),
                                setRepeatView.findViewById(R.id.wednesday),
                                setRepeatView.findViewById(R.id.thursday),
                                setRepeatView.findViewById(R.id.friday),
                                setRepeatView.findViewById(R.id.saturday),
                                setRepeatView.findViewById(R.id.sunday)};
                        new AlertDialog.Builder(getContext())
                                .setView(setRepeatView)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @SuppressWarnings("StringConcatenationInLoop")
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        for (int i = 0; i < 7; i++) {
                                            if (weekday[i].isChecked())
                                                repeat += "1";
                                            else
                                                repeat += "0";
                                        }
                                    }
                                })
                                .show();
                    }
                });
                pickupTimeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                new AlertDialog.Builder(getContext())
                        .setTitle("新增定時發送時間")
                        .setView(alarmInfoView)
                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        });*/
        return view;
    }

    public static void alarmAdd(String name, String type) {
        Prefs.putString(name, type);
        alarm.add(name.substring(6));
        alarmRecyclerView.setAdapter(alarmAdapter);
    }

    public class AlarmAdapter extends RecyclerView.Adapter<AlarmFragment.AlarmAdapter.ViewHolder> {

        private List<String> mData;

        AlarmAdapter(List<String> data) {
            mData = data;
        }

        // 建立ViewHolder
        class ViewHolder extends RecyclerView.ViewHolder {
            // 宣告元件
            private Switch alarmSwitch;

            ViewHolder(View itemView) {
                super(itemView);
                alarmSwitch = itemView.findViewById(R.id.alarmSwitch);
            }
        }

        @Override
        public AlarmFragment.AlarmAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 連結項目布局檔list_item
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_item_list, parent, false);
            return new AlarmFragment.AlarmAdapter.ViewHolder(view);
        }

        //acs712
        @Override
        public void onBindViewHolder(AlarmFragment.AlarmAdapter.ViewHolder holder, int position) {
            // 設置元件要顯示的內容
            holder.alarmSwitch.setText(mData.get(position).substring(6));

        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
