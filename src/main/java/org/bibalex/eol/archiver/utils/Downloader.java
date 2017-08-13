package org.bibalex.eol.archiver.utils;

import org.apache.commons.io.FileUtils;
import org.bibalex.eol.archiver.controllers.RestAPIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.BA_Proxy;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;


/**
 * Created by hduser on 7/27/17.
 */
public class Downloader {


    private static final Logger logger = LoggerFactory.getLogger(RestAPIController.class);
    private BA_Proxy proxy;
    private static final int BUFFER_SIZE = 4096;

    public Downloader(BA_Proxy proxy) {
        this.proxy = proxy;
        setProxy();
    }


    public boolean downloadFromUrl2(String fileURL, String dir) {
        boolean success = true;
        try {
            logger.info("File (" + fileURL + ") to be downloaded");
            URL httpurl = new URL(fileURL + "?_=" + System.currentTimeMillis());
            setProxy();
            String fileName = getFileNameFromUrl(fileURL);
            File f = new File(dir + fileName);
            FileUtils.copyURLToFile(httpurl, f);
        } catch (Exception e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: No file to download. ");
        }
        return success;
    }

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

    public void downloadUsingHttpClient(String url, String dir) {
//        HttpClient httpclient = new DefaultHttpClient();
//        HttpGet httpget = new HttpGet(urltofetch);
//        HttpResponse response = httpclient.execute(httpget);
//        HttpEntity entity = response.getEntity();
//        if (entity != null) {
//            long len = entity.getContentLength();
//            InputStream inputStream = entity.getContent();
//            // write the file to whether you want it.
//        }
    }

    private static String getFileNameFromUrl(String url) {
        String name = new Long(System.currentTimeMillis()).toString();
        int index = url.lastIndexOf("/");
        if (index > 0) {
            name = url.substring(index + 1);
            if (name.trim().length() > 0) {
                return name;
            }
        }
        return name;
    }

    public String downloadFromUrl(String fileURL, String saveDir) {
        URL url = null;
        boolean success = true;
        HttpURLConnection httpConn = null;
        String encodedURL = encodeURL(fileURL);
        String saveFilePath = null;
        logger.debug("URL:" + fileURL + " --> Encoded:" + encodedURL);

        try {
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
                saveFilePath = saveDir + File.separator + encodedURL;

                saveURLStream(inputStream, saveFilePath);

                logger.info("File (" + fileURL + ") is downloaded");
            } else {
                success = false;
                logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: No file to download. Server replied HTTP code: " + responseCode);
            }


        } catch (MalformedURLException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getMessage());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getStackTrace().toString());
            e.printStackTrace();
        } catch (IOException e) {
            success = false;
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("CLOSINGGGGGGGGGGGGGGGGGGGGGG");
            httpConn.disconnect();
        }

        return saveFilePath + "," + success;
    }

    private String encodeURL(String url) {
        return UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }

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

    public static void main(String[] args) {

    }
}
