package com.android.gpstest.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Downloads;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.gpstest.Application;
import com.android.gpstest.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A base implementation of a GNSS logger to store information to a file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public abstract class BaseFileLogger implements FileLogger {

    protected final String TAG = this.getClass().getName();
    protected static final String FILE_PREFIX = "gnss_log";

    protected static final String DIRECTORY = "Download/GPSTest";

    protected final Context context;

    protected BufferedWriter fileWriter;
    protected File file;
    protected boolean isStarted = false;
    protected File baseDirectory;

    public BaseFileLogger(Context context) {
        this.context = context;
    }

    public File getFile() {
        return file;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Defines the file extension to be used in implementations, following the ".". So "json" would
     * be used for files with the ".json" extension.
     * @return the file extension to be used, following the ".". So "json" would
     *      * be used for files with the ".json" extension.
     */
    abstract String getFileExtension();

    /**
     * Initialize file by adding a header, if desired for the given implementation
     *
     * @param writer   writer to use when writing file
     * @param filePath path to the current file
     */
    abstract void writeFileHeader(BufferedWriter writer, String filePath);

    /**
     * Called after files have finished initialing within startLog() but prior to returning from startLog(), if
     * additional init is required for a specific file logging implementation
     * @param fileWriter
     * @param isNewFile true if the file is new, or false if it already existed and was re-opened
     * @return true if the operation was successful, false if it was not
     */
    abstract boolean postFileInit(BufferedWriter fileWriter, boolean isNewFile);

    /**
     * Start a file logging process
     *
     * @param existingFile The existing file if file logging is to be continued, or null if a
     *                     new file should be created.
     * @param date The date and time to use for the file name
     * @return true if a new file was created, false if an existing file was used
     */
    public synchronized boolean startLog(File existingFile, Date date) {
        boolean isNewFile = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            baseDirectory = new File(context.getExternalFilesDir(null), FILE_PREFIX);
            baseDirectory.mkdirs();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            logError("Cannot write to external storage.");
            return false;
        } else {
            logError("Cannot read external storage.");
            return false;
        }

        String currentFilePath;

        if (existingFile != null) {
            // Use existing file
            currentFilePath = existingFile.getAbsolutePath();
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(existingFile, true));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return false;
            }
            if (!closeOldFileWriter()) {
                return false;
            }
            file = existingFile;
            fileWriter = writer;
            isNewFile = false;
        } else {
            // Create new logging file
            SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
            String fileName = String.format("%s_%s." + getFileExtension(), FILE_PREFIX, formatter.format(date));
            File currentFile = new File(baseDirectory, fileName);
            currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(currentFile, true));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return false;
            }

            writeFileHeader(writer, currentFilePath);

            if (!closeOldFileWriter()) {
                return false;
            }

            file = currentFile;
            fileWriter = writer;

            Log.d(TAG, Application.Companion.getApp().getString(R.string.logging_to_new_file, currentFilePath));
            isNewFile = true;
        }

        boolean postInit = postFileInit(fileWriter, isNewFile);
        if (!postInit) {
            return false;
        }

        isStarted = true;
        return isNewFile;
    }

    private boolean closeOldFileWriter() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                logException(Application.Companion.getApp().getString(R.string.unable_to_close_all_file_streams), e);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the logger is already started, or false if it is not
     *
     * @return
     */
    public synchronized boolean isStarted() {
        return isStarted;
    }

    public synchronized void close() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
                isStarted = false;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && file != null) {
            copyFileToDownloads(file);
        }
    }

    protected void logException(String errorMessage, Exception e) {
        Log.e(TAG, errorMessage, e);
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
    }

    protected void logError(String errorMessage) {
        Log.e(TAG, errorMessage);
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
    }

    @RequiresApi(Build.VERSION_CODES.R)
    protected void copyFileToDownloads(File fileToCopy) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileToCopy.getName());
        contentValues.put(Downloads.RELATIVE_PATH, DIRECTORY);
        Uri fileUri =
            contentResolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues);
        try (OutputStream outputStream =
            contentResolver.openOutputStream(fileUri)) {
            Files.copy(fileToCopy.toPath(), outputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error while writing to Downloads folder:", e);
        }
    }
}
