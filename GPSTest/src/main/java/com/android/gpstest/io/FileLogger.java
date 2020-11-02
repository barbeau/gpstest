package com.android.gpstest.io;

import java.io.File;

/**
 * Interface for file loggers
 */
public interface FileLogger {

    File getFile();

    void close();
}
