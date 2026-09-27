package com.example.sicsrtimetable;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

class Timetable {
    private final static String URL = "http://time-table.sicsr.ac.in/report.php";

    private final static Pattern PATTERN = Pattern.compile("<option value=\"([a-zA-Z])\">([^<]+)</option>");

    public static LinkedHashMap<CharSequence, Batch> getBatches() {
        var batches = new LinkedHashMap<CharSequence, Batch>();
        var matcher = PATTERN.matcher(Http.getMethod(URL));

        while (matcher.find()) {
            var string = matcher.group(2);
            var value = matcher.group(1);
            var batch = new Batch(string, value);
            batches.put(string, batch);
        }

        return batches;
    }

    public static final class Batch {
        private final static SimpleDateFormat MILITARY = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);

        private final static SimpleDateFormat ANALOG = new SimpleDateFormat("h:mm a", Locale.ROOT);

        private final static String FORMAT = "\n%s\n\n🚪 %s\n🕧 %s\n🕝 %s\n";

        private final static List<String> EMPTY = Collections.singletonList(String.format(FORMAT, '❓', '❓', '❓', '❓'));

        private final static String URL = "http://time-table.sicsr.ac.in/report.php?from_day=%d&from_month=%d&from_year=%d&to_day=%d&to_month=%d&to_year=%d&match_confirmed=1&output=0&output_format=1&sortby=s&sumby=t&phase=2&datatable=1%s";

        private static final String PARAMETER = "&typematch[]=%s";

        private final String string, value;

        public Batch(String string, String value) {
            this.string = string;
            this.value = value;
        }

        @NonNull
        @Override
        public String toString() {
            return string;
        }

        private static String $(String string) {
            return string.substring(1, string.length() - 1);
        }

        private static String getUrl(Iterable<Batch> batches, Calendar calendar) {
            var builder = new StringBuilder();
            for (var batch : batches) builder.append(String.format(PARAMETER, batch.value));

            var year = calendar.get(Calendar.YEAR);
            var month = calendar.get(Calendar.MONTH) + 1;
            var day = calendar.get(Calendar.DAY_OF_MONTH);

            return String.format(Locale.ROOT, URL, day, month, year, day, month, year, builder);
        }

        public static List<String> getCourses(Iterable<Batch> batches, Calendar calendar) {
            var iterator = batches.iterator();
            if (!iterator.hasNext()) return EMPTY;

            var courses = new ArrayList<String>();
            var iterable = Http.getMethod(getUrl(batches, calendar)).split("\\R");
            if (iterable.length == 0) return EMPTY;

            for (int index = 1; index < iterable.length; index++) {
                var array = iterable[index].split(",");

                var description = $(array[6]);
                var room = $(array[2]);

                var start = $(array[3]).split("-")[0];
                var end = $(array[4]).split("-")[0];

                try {
                    start = ANALOG.format(Objects.requireNonNull(MILITARY.parse(start)));
                } catch (ParseException ignore) {
                }

                try {
                    end = ANALOG.format(Objects.requireNonNull(MILITARY.parse(end)));
                } catch (ParseException ignore) {
                }

                var course = String.format(FORMAT, description, room, start, end);
                courses.add(course);
            }

            return courses.isEmpty() ? EMPTY : courses;
        }
    }
}
