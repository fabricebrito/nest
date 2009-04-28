/*
 * $Id: FileDownloader.java,v 1.1 2009-04-28 14:39:32 lveci Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.io;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ProgressMonitor;

/**
 * A utility class for downloading files from the internet.
 * @author Norman Fomferra
 * @version
 */
public class FileDownloader {
    // todo (nf) - dont use progress monitor directly, use an observer instead
    
    /**
     * Downloads a file from the specified URL to the specified local target directory.
     * The method uses a Swing progress monitor to visualize the download process.
     * @param fileUrl the URL of the file to be downloaded
     * @param targetDir  the target directory
     * @param parentComponent the parent component to be used by the progress monitor
     * @throws IOException if an I/O error occurs
     */
    public static File downloadFile(final URL fileUrl,
                                    final File targetDir,
                                    final Component parentComponent) throws IOException {
        final File outputFile = new File(targetDir, new File(fileUrl.getFile()).getName());
        final URLConnection urlConnection = fileUrl.openConnection();
        final int contentLength = urlConnection.getContentLength();
        final String message = "Downloading data from " + fileUrl + " to " + outputFile;   /*I18N*/
        final ProgressMonitor progressMonitor = new ProgressMonitor(parentComponent, message, "", 0, 100);
        final InputStream is = new BufferedInputStream(new ObservableInputStream(urlConnection.getInputStream(), contentLength, new ProgressMonitorInputStreamObserver(progressMonitor)));
        final OutputStream os;
        try {
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (IOException e) {
            is.close();
            throw e;
        }

        try {
            IOUtils.copyBytesAndClose(is, os);
        } catch (IOException e) {
            outputFile.delete();
            throw e;
        } finally {
            progressMonitor.close();
        }

        return outputFile;
    }

}
