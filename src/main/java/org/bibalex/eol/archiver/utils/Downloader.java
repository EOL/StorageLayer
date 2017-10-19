package org.bibalex.eol.archiver.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bibalex.eol.archiver.controllers.RestAPIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.BA_Proxy;
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;


/**
 * Created by maha.mostafa on 7/27/17.
 */
public class Downloader {


    private static final Logger logger = LoggerFactory.getLogger(RestAPIController.class);
    private BA_Proxy proxy;
    private static final int BUFFER_SIZE = 4096; //4MG or 8192 8MG

    public Downloader(BA_Proxy proxy) {
        this.proxy = proxy;
        setProxy();
    }

    /**
     * Sets up the storage layer proxy settings, if exist
     */
    private void setProxy() {
        if(proxy.isProxyExists()) {
            System.setProperty("http.proxyHost", proxy.getProxy());
            System.setProperty("http.proxyPort", proxy.getPort());
            System.setProperty("https.proxyHost", proxy.getProxy());
            System.setProperty("https.proxyPort", proxy.getPort());

            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    proxy.getUserName(), proxy.getPassword().toCharArray());
                        }
                    }
            );

            System.setProperty("https.proxyUser", proxy.getUserName());
            System.setProperty("https.proxyPassword", proxy.getPort());
            System.setProperty("http.proxyUser", proxy.getUserName());
            System.setProperty("http.proxyPassword", proxy.getPort());
        }
    }

    /**
     * Extracts the file name of the uploaded URL
     * @param url the input file URL
     * @return the name of the file, if exist. Otherwise it will generate a name consisting of the current timestamp
     */
    private static String getFileNameFromUrl(String url) {
        String name = new Long(System.currentTimeMillis()).toString();
        int index = url.lastIndexOf("/"); // Normal URL slash
        if (index > 0) {
            name = url.substring(index + 1);
            if (name.trim().length() > 0) {
                return name;
            }
        }
        return name;
    }

    /**
     * Downloads the file from the input URL and saves it in the input directory.
     * @param fileURL the URL of the file.
     * @param saveDir the directory where to save the downloaded URL.
     * @return a string of both the new file directory and the uploading status separated by ,
     */
    public String downloadFromUrl(String fileURL, String saveDir) {
        URL url = null;
        boolean success = true;
        HttpURLConnection httpConn = null;
        String encodedURL = encodeURL(fileURL);
        logger.debug("URL:" + fileURL + " --> Encoded:" + encodedURL);

        String saveFilePath = saveDir + encodedURL;
        logger.debug("saveFilePath:" + saveFilePath);


        try {
            // Check if directory exists
            if (Files.exists(Paths.get(saveFilePath))) {
                logger.info("Media file already exists ..");
            } else {
                logger.info("File (" + fileURL + ") to be downloaded");
                url = new URL(fileURL);

                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDefaultUseCaches(false);
                httpConn.setRequestProperty("Pragma", "no-cache");
                httpConn.setRequestProperty("Expires", "0");

                int responseCode = httpConn.getResponseCode();
                // check HTTP response code first
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // URL Extra Info
    //                String fileName = "";
    //                String disposition = httpConn.getHeaderField("Content-Disposition");
    //                String contentType = httpConn.getContentType();
    //                int contentLength = httpConn.getContentLength();
    //
    //                if (disposition != null) {
    //                    // extracts file name from header field
    //                    int index = disposition.indexOf("filename=");
    //                    if (index > 0) {
    //                        fileName = disposition.substring(index + 10,
    //                                disposition.length() - 1);
    //                    }
    //                } else {
    //                    // extracts file name from URL
    //                    fileName = getFileNameFromUrl(fileURL);
    //                }

                    // opens input stream from the HTTP connection
                    InputStream inputStream = httpConn.getInputStream();


                    // saves the input stream of the file URL
                    saveURLStream(inputStream, saveFilePath);

                    logger.info("File (" + fileURL + ") is downloaded");
                } else {
                    success = false;
                    logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: No file to download. Server replied HTTP code: " + responseCode);
                }
            }
        } catch (MalformedURLException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getMessage());
            ExceptionUtils.getStackTrace(e);
        } catch (FileNotFoundException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getMessage());
        } catch (IOException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getMessage());
        } finally {
            if(httpConn != null)
                httpConn.disconnect();
        }

        return saveFilePath + "," + success;
    }

    /**
     * Encodes the url string to another name to be saved with it.
     * @param url the input URL.
     * @return the encoded string
     */
    private String encodeURL(String url) {
        return UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }

    /**
     * Saves the downloaded input stream into file.
     * @param inputStream the input URL stream.
     * @param saveFilePath the directory where to save the file.
     * @throws IOException
     */
    private void saveURLStream(InputStream inputStream, String saveFilePath) throws IOException {
        // Using java IO
//        // opens an output stream to save into file
//        FileOutputStream outputStream = new FileOutputStream(saveFilePath);
//        int bytesRead = -1;
//        byte[] buffer = new byte[BUFFER_SIZE];
//        while ((bytesRead = inputStream.read(buffer)) != -1) {
//            outputStream.write(buffer, 0, bytesRead);
//        }
//        outputStream.close();
////        inputStream.close();

        // Using Java NIO
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(saveFilePath);
        fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
        fos.close();
//        channel.close();
    }

}
