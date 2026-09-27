package com.example.sicsrtimetable;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

import android.system.*;

public class Http {
    public final static int SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);

    private static String readContent(InputStream stream) throws IOException {
        var length = 0;
        var buffer = new byte[SIZE];
        var string = new ByteArrayOutputStream();

        while ((length = stream.read(buffer, 0, buffer.length)) != -1)
            string.write(buffer, 0, length);

        return string.toString();
    }

    public static String getMethod(String url) {
        while (true) try (var stream = new URL(url).openStream()) {
            return readContent(stream);
        } catch (IOException ignore) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static String postMethod(String url, String content) {
        while (true) {
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                connection.setDoOutput(true);
                try (var stream = connection.getOutputStream()) {
                    stream.write(content.getBytes());
                }

                return readContent(connection.getInputStream());
            } catch (IOException ignore) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
