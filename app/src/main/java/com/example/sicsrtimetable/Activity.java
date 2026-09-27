package com.example.sicsrtimetable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Activity extends AppCompatActivity {

    private DatePickerDialog date;

    private ProgressBar progress;

    private Button select;

    private LinkedHashMap<CharSequence, Timetable.Batch> batches;

    private final LinkedHashMap<CharSequence, Timetable.Batch> selected = new LinkedHashMap<>();

    private CharSequence[] items;

    private boolean[] checked;

    private AlertDialog dialog;

    private View timeline;

    private Button jump, yesterday, tomorrow;

    private ListView courses;

    private ArrayAdapter<String> adapter;

    private final Calendar calendar = Calendar.getInstance();

    private final static SimpleDateFormat FORMAT = new SimpleDateFormat("EEEE, d MMMM y", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        var main = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        var year = calendar.get(Calendar.YEAR);
        var month = calendar.get(Calendar.MONTH);
        var day = calendar.get(Calendar.DAY_OF_MONTH);

        date = new DatePickerDialog(this, new OnDateSetListener(), year, month, day);
        date.setOnShowListener($ ->
        {
            var button = date.getButton(DialogInterface.BUTTON_NEGATIVE);
            button.setVisibility(View.GONE);

            button = date.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setText("◀");
        });
        date.setCancelable(false);

        progress = findViewById(R.id.progress);
        select = findViewById(R.id.select);
        jump = findViewById(R.id.jump);

        courses = findViewById(R.id.courses);

        timeline = findViewById(R.id.timeline);
        yesterday = findViewById(R.id.yesterday);
        tomorrow = findViewById(R.id.tomorrow);

        setEnable(select, false);
        setEnable(yesterday, false);
        setEnable(jump, false);
        setEnable(tomorrow, false);

        jump.setOnClickListener($ -> date.show());
        yesterday.setOnClickListener($ -> offsetDay(-1));
        tomorrow.setOnClickListener($ -> offsetDay(1));

        CompletableFuture.runAsync(this::onCreate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try (var stream = openFileOutput(".bin", MODE_PRIVATE)) {
            var output = new ObjectOutputStream(stream);
            var object = new HashSet<>(selected.keySet());
            output.writeObject(object);
        } catch (Exception ignore) {
        }
    }

    static void setEnable(View view, boolean value) {
        view.setEnabled(value);
        view.setClickable(value);
        view.setFocusable(value);
    }

    void onCreate() {
        batches = Timetable.getBatches();
        items = batches.keySet().toArray(new CharSequence[0]);
        checked = new boolean[items.length];

        try (var stream = openFileInput(".bin")) {
            var input = new ObjectInputStream(stream);
            var object = (HashSet<CharSequence>) input.readObject();

            for (var index = 0; index < items.length; index++) {
                var item = items[index];
                if (!object.contains(item)) continue;

                var batch = batches.get(item);
                selected.put(item, batch);
                checked[index] = true;
            }

        } catch (Exception ignore) {
        }

        runOnUiThread(() -> {
            dialog = createDialog();
            select.setOnClickListener($ -> dialog.show());

            this.adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
            courses.setAdapter(this.adapter);

            setDate();

            jump.setVisibility(View.VISIBLE);
            select.setVisibility(View.VISIBLE);
            timeline.setVisibility(View.VISIBLE);

            setCourses();
        });
    }

    void setCourses() {
        setEnable(select, false);
        setEnable(yesterday, false);
        setEnable(jump, false);
        setEnable(tomorrow, false);

        progress.setVisibility(View.VISIBLE);
        courses.setVisibility(View.INVISIBLE);

        CompletableFuture.runAsync(() -> {
            var collection = Timetable.Batch.getCourses(selected.values(), calendar);

            runOnUiThread(() -> {
                adapter.setNotifyOnChange(false);
                adapter.clear();
            });

            for (var course : collection)
                runOnUiThread(() -> adapter.add(course));

            runOnUiThread(() -> {
                adapter.setNotifyOnChange(true);
                adapter.notifyDataSetChanged();

                courses.setSelection(0);
                courses.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);

                setEnable(select, true);
                setEnable(yesterday, true);
                setEnable(jump, true);
                setEnable(tomorrow, true);
            });
        });
    }

    AlertDialog createDialog() {
        var builder = new AlertDialog.Builder(this);
        builder = builder.setPositiveButton("◀", (sender, args) -> setCourses());
        builder = builder.setMultiChoiceItems(items, checked, this::onMultiChoiceClick);
        builder.setCancelable(false);
        return builder.create();
    }

    void onMultiChoiceClick(DialogInterface sender, int index, boolean checked) {
        var key = items[index];
        var batch = batches.get(key);
        if (checked) selected.put(key, batch);
        else selected.remove(key);
    }

    void offsetDay(int amount) {
        calendar.add(Calendar.DAY_OF_MONTH, amount);
        setDate();
        setCourses();
    }

    void setDate() {
        var time = calendar.getTime();
        var text = FORMAT.format(time);
        jump.setText(text);
    }

    private final class OnDateSetListener implements DatePickerDialog.OnDateSetListener {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            calendar.set(year, month, day);
            setDate();
            setCourses();
        }
    }
}