package org.bibalex.eol.archiver.utils;

import fi.solita.clamav.ClamAVClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
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

import static org.bibalex.eol.archiver.controllers.RestAPIController.hostName;
import static org.bibalex.eol.archiver.controllers.RestAPIController.maximumFileSize;
import static org.bibalex.eol.archiver.controllers.RestAPIController.tcpPortNumber;
import static org.bibalex.eol.archiver.controllers.RestAPIController.mediaTempPath;



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
        if (proxy.isProxyExists()) {
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
     *
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
     *
     * @param fileURL the URL of the file.
     * @param saveDir the directory where to save the downloaded URL.
     * @return a string of both the new file directory and the uploading status separated by ,
     */
    public String downloadFromUrl(String fileURL, String saveDir, String type) {
        URL url = null;
        boolean success = true;
        HttpURLConnection httpConn = null;
        String encodedURL = encodeURL(fileURL);
        logger.debug("URL:" + fileURL + " --> Encoded:" + encodedURL);

        String saveFilePath = saveDir + encodedURL;
        String tempSaveFilePath = mediaTempPath + "/"+encodedURL;
        File tempPath = new File(mediaTempPath);
        if(!tempPath.exists())
            tempPath.mkdir();
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
                     if (checkSecurityConcerns(url, httpConn, type, tempSaveFilePath)) {
                         File savedMedia = new File(tempSaveFilePath);
                         if(!savedMedia.exists())
                             savedMedia.createNewFile();
                         savedMedia.renameTo(new File(saveFilePath));
                            logger.info("File (" + fileURL + ") is downloaded");
                            checkMediaTypetoResize(saveFilePath);
                        } else {
                            success = false;
                            return saveFilePath + ", " + success;
//                            }
                        }
                    }
                else {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null)
                httpConn.disconnect();
        }

        return saveFilePath + "," + success;
    }

    /**
     * Encodes the url string to another name to be saved with it.
     *
     * @param url the input URL.
     * @return the encoded string
     */
    private String encodeURL(String url) {
        return UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }

    /**
     * Saves the downloaded input stream into file.
     *
     * @param inputStream  the input URL stream.
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
//        inputStream.close();

        // Using Java NIO
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(saveFilePath);
        fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
        fos.close();
        channel.close();
    }

    private void checkMediaTypetoResize(String filePath) throws IOException, MimeTypeException {

        File mediaFile = new File(filePath);
        ImageResizer imageResizer = new ImageResizer();
        Tika fileTypeDetector = new Tika();
        String fileType = fileTypeDetector.detect(mediaFile);
        TikaConfig config = TikaConfig.getDefaultConfig();
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(mediaFile));
        MediaType mediaType = config.getMimeRepository().detect(stream, new Metadata());
        MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
        String extension = mimeType.getExtension();
        if (fileType.contains("image"))
            imageResizer.createThumbnail(mediaFile.getPath(), extension);
    }

    private boolean checkSecurityConcerns(URL request, HttpURLConnection response, String type, String scanDir) throws IOException {
       return(checkMediaTypeMatch(request, response, type) && checkFileSize(response) && scanDownloadedFile(response, scanDir));
    }

    private boolean scanDownloadedFile(HttpURLConnection response, String filePath) throws IOException {
        System.out.println("=============== Scanning Response: ================");
        saveURLStream(response.getInputStream(), filePath);
        ClamAVClient cl = new ClamAVClient(hostName, tcpPortNumber);
        byte[] reply;
        File fileToScan = new File(filePath);
            reply = cl.scan(new FileInputStream(fileToScan));
            if (!ClamAVClient.isCleanReply(reply))
            {
                logger.error("ClamaAV Scanner: Malicious File Detected-- Deleting File");
                fileToScan.delete();
                return false;
            }
            logger.info("ClamAV Scanner: File marked as safe");
        return true;
    }

    private boolean checkMediaTypeMatch(URL request, HttpURLConnection response, String type) throws IOException {
        String responseContentType = response.getContentType();
        Tika fileTypeDetector = new Tika();
        String requestContentType = fileTypeDetector.detect(request);
        System.out.println("Request Content Type: " + requestContentType);
        System.out.println("Response Content Type: " + responseContentType);
        if ((!(responseContentType.equalsIgnoreCase(requestContentType))) || (!(responseContentType.equalsIgnoreCase(type)) && (!(type.equalsIgnoreCase(""))))) {
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: Expected Media Format mismatch");
        return false;
    }
    return true;
    }

    private boolean checkFileSize(HttpURLConnection response){
        if(response.getContentLength() > maximumFileSize)
        {
            logger.error("org.bibalex.eol.archiver.utils.Downloader.downloadFromUrl: Media File is too large to download; Download Aborted\nPlease select a file smaller than " + maximumFileSize / 1048576 + " MB");
            return false;
        }
        return true;
    }
}
