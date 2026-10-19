package com.example.lunatracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private EditText etDateInput;
    private Button btnSaveDate;
    private TextView tvStatusMain, tvStatusSub, tvAvgCycle, tvTotalLogs, tvHistoryList;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LunaTrackerPrefs";
    private static final String KEY_LOGS = "cycle_logs";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etDateInput = findViewById(R.id.etDateInput);
        btnSaveDate = findViewById(R.id.btnSaveDate);
        tvStatusMain = findViewById(R.id.tvStatusMain);
        tvStatusSub = findViewById(R.id.tvStatusSub);
        tvAvgCycle = findViewById(R.id.tvAvgCycle);
        tvTotalLogs = findViewById(R.id.tvTotalLogs);
        tvHistoryList = findViewById(R.id.tvHistoryList);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        btnSaveDate.setOnClickListener(v -> saveNewDateEntry());

        refreshUI();
    }

    private void saveNewDateEntry() {
        String inputStr = etDateInput.getText().toString().trim();
        if (inputStr.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date parsedDate = dateFormat.parse(inputStr);
            if (parsedDate == null) throw new ParseException("Invalid", 0);

            Set<String> savedLogs = sharedPreferences.getStringSet(KEY_LOGS, new HashSet<>());
            Set<String> updatedLogs = new HashSet<>(savedLogs);
            updatedLogs.add(dateFormat.format(parsedDate));

            sharedPreferences.edit().putStringSet(KEY_LOGS, updatedLogs).apply();

            etDateInput.setText("");
            Toast.makeText(this, "Cycle entry saved successfully", Toast.LENGTH_SHORT).show();
            refreshUI();

        } catch (ParseException e) {
            Toast.makeText(this, "Use correct format: YYYY-MM-DD", Toast.LENGTH_LONG).show();
        }
    }

    private void refreshUI() {
        Set<String> savedLogs = sharedPreferences.getStringSet(KEY_LOGS, new HashSet<>());
        if (savedLogs.isEmpty()) {
            tvStatusMain.setText("No cycle data logged");
            tvStatusSub.setText("Start tracking by logging her November start date above.");
            tvAvgCycle.setText("-- Days");
            tvTotalLogs.setText("0 entries");
            tvHistoryList.setText("No previous entries recorded.");
            return;
        }

        List<Date> sortedDates = new ArrayList<>();
        for (String dateStr : savedLogs) {
            try {
                sortedDates.add(dateFormat.parse(dateStr));
            } catch (ParseException ignored) {}
        }
        Collections.sort(sortedDates);

        tvTotalLogs.setText(sortedDates.size() + " entries");

        StringBuilder historyBuilder = new StringBuilder();
        for (int i = sortedDates.size() - 1; i >= 0; i--) {
            historyBuilder.append("• Started on: ").append(dateFormat.format(sortedDates.get(i))).append("\n");
        }
        tvHistoryList.setText(historyBuilder.toString().trim());

        if (sortedDates.size() > 1) {
            long totalDaysBetween = 0;
            for (int i = 0; i < sortedDates.size() - 1; i++) {
                long diffInMillies = sortedDates.get(i + 1).getTime() - sortedDates.get(i).getTime();
                totalDaysBetween += TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            }
            long averageCycle = totalDaysBetween / (sortedDates.size() - 1);
            tvAvgCycle.setText(averageCycle + " Days");

            Date lastDate = sortedDates.get(sortedDates.size() - 1);
            long nextCycleEstimatedMillis = lastDate.getTime() + (averageCycle * 24L * 60L * 60L * 1000L);
            Date estimatedNextDate = new Date(nextCycleEstimatedMillis);

            long diffToNext = estimatedNextDate.getTime() - System.currentTimeMillis();
            long daysRemaining = TimeUnit.DAYS.convert(diffToNext, TimeUnit.MILLISECONDS);

            if (daysRemaining >= 0) {
                tvStatusMain.setText("Expected in " + daysRemaining + " days");
                tvStatusSub.setText("Estimated start date: " + dateFormat.format(estimatedNextDate));
            } else {
                tvStatusMain.setText("Cycle Window Overdue / Active");
                tvStatusSub.setText("Last expected around " + dateFormat.format(estimatedNextDate) + ".");
            }
        } else {
            tvAvgCycle.setText("Pending...");
            Date singleDate = sortedDates.get(0);
            tvStatusMain.setText("November Baseline Set");
            tvStatusSub.setText("Logged date: " + dateFormat.format(singleDate) + ".");
        }
    }
}
