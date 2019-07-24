package com.mapleleaf.irremote;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment {

    View view;
    static RecyclerView historyRecyclerView;
    static HistoryAdapter historyAdapter;
    static ArrayList<String> history = new ArrayList<>();
    private Spinner filterSpinner;
    private List<String> filterCategory = new ArrayList<String>();
    private ArrayAdapter<String> filterAdapter;

    String temp;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);
        for (LinkedHashMap.Entry<String, ?> entry : Prefs.getAll().entrySet()) {
            if (entry.getKey().contains("history,")) {
                history.add(entry.getKey().substring(8));
            }
        }
        sortByTime(history);
        // 連結元件
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        // 設置RecyclerView為列表型態
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 設置格線
        historyRecyclerView.addItemDecoration(new DividerItemDecoration(Objects.requireNonNull(getContext()) , DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        historyAdapter = new HistoryAdapter(history);
        // 設置adapter給recycler_view
        historyRecyclerView.setAdapter(historyAdapter);
        filterSpinner = view.findViewById(R.id.chooseSpinner);
        return view;
    }

    public static void historyAdd(String name, String type) {
        Prefs.putString(name, type);
        history.add(name.substring(8));
        sortByTime(history);
        historyRecyclerView.setAdapter(historyAdapter);
    }

    public static void sortByTime(ArrayList<String> arrayList) {
        Collections.sort(arrayList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.substring(o2.indexOf(",")+1,o2.lastIndexOf(",") ) .compareTo(o1.substring(o1.indexOf(",")+1,o1.lastIndexOf(",") ) );
            }
        });
    }

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private List<String> mData;

        HistoryAdapter(List<String> data) {
            mData = data;
        }

        // 建立ViewHolder
        class ViewHolder extends RecyclerView.ViewHolder {
            // 宣告元件
            private TextView name;
            private TextView date;
            private TextView time;
            private TextView mode;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                date = itemView.findViewById(R.id.date);
                mode = itemView.findViewById(R.id.mode);
                time = itemView.findViewById(R.id.time);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 連結項目布局檔list_item
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.histroy_item_list, parent, false);
            return new ViewHolder(view);
        }

        //acs712
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // 設置txtItem要顯示的內容
            temp = mData.get(position);
            holder.name.setText(temp.substring(0, temp.indexOf(",")));
            holder.date.setText(temp.substring(temp.indexOf(",") + 1, temp.lastIndexOf("/")));
            holder.time.setText(temp.substring(temp.lastIndexOf("/") + 1, temp.lastIndexOf(",")));
            holder.mode.setText(temp.substring(temp.lastIndexOf(",") + 1));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
