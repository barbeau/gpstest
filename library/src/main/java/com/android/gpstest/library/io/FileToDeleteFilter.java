package com.android.gpstest.library.io;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a {@link FileFilter} to delete files that are not in the
 * {@link FileToDeleteFilter#mRetainedFiles}.
 */
public class FileToDeleteFilter implements FileFilter {
    private final List<File> mRetainedFiles;

    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    /**
     *
     * @param retainedFiles List of files to not delete
     */
    public FileToDeleteFilter(File... retainedFiles) {
        this.mRetainedFiles = Arrays.asList(retainedFiles);
    }

    /**
     * Returns {@code true} to delete the file, and {@code false} to keep the file.
     *
     * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
     */
    @Override
    public boolean accept(File pathname) {
        if (pathname == null || !pathname.exists()) {
            return false;
        }
        if (mRetainedFiles.contains(pathname)) {
            return false;
        }
        return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
    }
}