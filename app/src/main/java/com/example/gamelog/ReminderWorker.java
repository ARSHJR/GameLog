package com.example.gamelog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.TimeZone;

public class ReminderWorker extends Worker {

    private static final String CHANNEL_ID = "game_reminder_channel";
    private static final String CHANNEL_NAME = "Game Reminders";
    public static final String INPUT_NOTE_ID = "input_note_id";
    public static final String INPUT_GAME_TITLE = "input_game_title";
    public static final String INPUT_REMINDER_TITLE = "input_reminder_title";
    public static final String INPUT_REMINDER_BODY = "input_reminder_body";
    public static final String INPUT_TIMEZONE_ID = "input_timezone_id";
    private static final int DEFAULT_NOTIFICATION_ID = 1001;

    private static final int QUIET_START_MINUTES = 22 * 60 + 30; // 22:30
    private static final int QUIET_END_MINUTES   = 8 * 60;      // 08:00

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        createNotificationChannel();
        String timezoneId = getInputData().getString(INPUT_TIMEZONE_ID);
        if (!isInQuietHours(timezoneId)) {
            showNotification();
        }
        return Result.success();
    }

    private boolean isInQuietHours(String timezoneId) {
        if (timezoneId == null || timezoneId.trim().isEmpty()) {
            return false;
        }
        try {
            String trimmedId = timezoneId.trim();
            TimeZone tz = TimeZone.getTimeZone(trimmedId);
            // getTimeZone() silently returns GMT for unrecognised IDs; treat as invalid
            if ("GMT".equals(tz.getID()) && !"GMT".equalsIgnoreCase(trimmedId)) {
                return false;
            }
            Calendar cal = Calendar.getInstance(tz);
            int minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            // Quiet hours span midnight: on if >= 22:30 or < 08:00
            return minutesOfDay >= QUIET_START_MINUTES || minutesOfDay < QUIET_END_MINUTES;
        } catch (Exception e) {
            return false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Periodic reminders to update your game progress");
            NotificationManager manager = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification() {
        String reminderTitle = getInputData().getString(INPUT_REMINDER_TITLE);
        String reminderBody = getInputData().getString(INPUT_REMINDER_BODY);
        String gameTitle = getInputData().getString(INPUT_GAME_TITLE);
        String noteId = getInputData().getString(INPUT_NOTE_ID);

        String title = (reminderTitle != null && !reminderTitle.trim().isEmpty())
                ? reminderTitle.trim()
                : "GameLog Reminder";

        String body;
        if (reminderBody != null && !reminderBody.trim().isEmpty()) {
            body = reminderBody.trim();
        } else if (gameTitle != null && !gameTitle.trim().isEmpty()) {
            body = "Reminder for " + gameTitle.trim();
        } else {
            body = "Don't forget to update your game progress";
        }

        int notificationId = DEFAULT_NOTIFICATION_ID;
        if (noteId != null && !noteId.trim().isEmpty()) {
            notificationId = Math.abs(noteId.hashCode());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}
