package org.bibalex.eol.archiver.services;

/**
 * Created by maha.mostafa on 4/18/17.
 */

import model.BA_Proxy;
import org.apache.commons.io.FileUtils;
import org.bibalex.eol.archiver.utils.Constants;
import org.bibalex.eol.archiver.utils.Downloader;
import org.bibalex.eol.archiver.utils.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

@Service
public class ArchivesService {

    private static final Logger logger = LoggerFactory.getLogger(ArchivesService.class);
    private FileManager fileManager;


    public ArchivesService() {
        this.fileManager = new FileManager();
        logger.debug("ArchivesService Testing INIT");
    }

    /**
     * Saves the input resource file on disk, if it was found before, it will be replaced.
     *
     * @param uploadedFile the uploaded file.
     * @param basePath     the directory of where to save it.
     * @param resId        the resource id of the uploaded file.
     * @param isOrg        1 if it is the uploaded file, 0 if it is DWCA.
     * @return true if the saving of the resource in its right directory was successful
     */
    public boolean saveUploadedArchive(MultipartFile uploadedFile, String basePath, String resId, String isOrg) {
        try {
            boolean succeeded = true;
            String prefix = isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) ? Constants.ORG_START : Constants.CORE_START;

            String fullPath = basePath + File.separator + resId + File.separator + prefix + "_" + uploadedFile.getOriginalFilename();

            Path directoryPath = Paths.get(basePath + File.separator + resId);
            Path filePath = Paths.get(fullPath);
            /**check if there is an already existing version of the resource archive, and rename it to resId_old if so*/
            if (directoryPath.toFile().exists()) {
                File dir = new File(String.valueOf(directoryPath));
                File oldDir = new File(dir.getPath() + "_old");
                // in case an older version does exist, delete it and rename the current version to old
                if (oldDir.exists()) {
                    FileUtils.deleteDirectory(oldDir);
                    oldDir = new File(dir.getPath() + "_old");
                }
                dir.renameTo(oldDir);
                logger.info("Found an Already Existing Version of Resource: " + resId);
                logger.info("Older Version Renamed to: " + oldDir.getPath());
            }

            if (Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
                Files.copy(uploadedFile.getInputStream(), filePath);

                // TODO delete it
                // create a duplicate DWCA until connectors are created
                if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) {
                    Files.copy(uploadedFile.getInputStream(), Paths.get(basePath + File.separator + resId + File.separator + "core" + "_" + uploadedFile.getOriginalFilename()));
                }
            } else {
                File dir = new File(basePath + File.separator + resId);
                File[] files = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith(prefix + "_");
                    }
                });

                // Check if core files exist
                File[] coreFiles = null;
                if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) {
                    File fdDir = new File(basePath + File.separator + resId);
                    coreFiles = dir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.startsWith("core" + "_");
                        }
                    });
                }

                // If there is another same type file exist
                if (files.length > 0) {
                    File file = files[0];

                    if (!file.delete()) {
                        logger.error("Error Deleting Resource: " + resId + "of Type: " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA "));
                        succeeded = false;
                    } else {
                        Files.copy(uploadedFile.getInputStream(), filePath);
                        logger.info("Resource: " + resId + " of Type: " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + "Already Exists");
                        logger.info("Resource Files Successfully Replaced");

                        // TODO delete it
                        // create a duplicate DWCA until connectors are created
                        if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) {
                            Files.copy(uploadedFile.getInputStream(), Paths.get(basePath + File.separator + resId + File.separator + "core" + "_" + uploadedFile.getOriginalFilename()));
                            if (coreFiles != null && coreFiles.length > 0) {
                                coreFiles[0].delete();
                            }
                        }

                    }
                } else {
                    logger.info(" Created New Resource: " + resId + " of Type: " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA "));
                    Files.copy(uploadedFile.getInputStream(), filePath);
                    // TODO
                    // create a duplicate DWCA until connectors are created
                    if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) {
                        Files.copy(uploadedFile.getInputStream(), Paths.get(basePath + File.separator + resId + File.separator + "core" + "_" + uploadedFile.getOriginalFilename()));
                        if (coreFiles != null && coreFiles.length > 0) {
                            coreFiles[0].delete();
                        }
                    }
                }
            }
            return succeeded;
        } catch (IOException e) {
            logger.error("IOException: Failed to Save File: " + uploadedFile.getOriginalFilename());
            logger.error("Stack Trace: ", e);
            return false;
        }
    }

    /**
     * Reads the resource file from its directory and returns it. If the DWCA doesn't exist so it will return the publishing uploaded file.
     *
     * @param basePath the base directory where all resources are saved.
     * @param resId    the id of the required resource.
     * @param isOrg    1 if the required file is the uploaded in the publishing layer, 0 if the required file is the DWCA
     * @return the required resource file.
     */

    public File getResourceFile(String basePath, String resId, String isOrg, String isNew) {
        File file = null,
        dir = new File(basePath + File.separator + resId + (isNew.equalsIgnoreCase("1")? "":"_old"));
        if(isNew.equals("0")&& (!dir.exists()))
            return file;

        File[] orgFiles, coreFiles;
        coreFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(Constants.CORE_START + "_");
            }
        });
        if (!isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) && coreFiles.length != 0) {
            // Core file
            file = coreFiles[0];
//            logger.info("Downloading DWCA File: " + file.getName() + " of Resource [" + resId + "] ..");
            logger.info("Resource: " + resId + "- Downloading DWCA File: " + file.getName());
        } else {
            orgFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(Constants.ORG_START + "_");
                }
            });

            if (orgFiles.length == 0) {
                logger.error("Resource: " + resId + "- No Original Resource to Download");
            } else {
                file = orgFiles[0];
                // Input stream resource
                if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE))
                    logger.info("Resource: " + resId + "- Downloading Original File: " + file.getName());
                else {
                    logger.info("Resource: " + resId + "- Attempting to Download DWCA File" + file.getName());
                    logger.info("No DWCA File Found for Resource: " + resId);
                    logger.info("Resource: " + resId + "- Downloading Original File: " + file.getName());
                    // uncomment if want to not download original when core is required
//                    file = null;
                }
            }
        }
        return file;
    }

    /**
     * Copy file from source to destination using stream.
     *
     * @param source the source file to be copied.
     * @param dest   the destination file to copy the data to.
     * @throws IOException
     */
    public void copyFile(File source, File dest) throws IOException {
        fileManager.customBufferBufferedStreamCopy(source, dest);
    }

    /**
     * Downloads the input media urls and saves them on disk.
     *
     * @param mediaURLs     a list of the URLs to be downloaded.
     * @param proxySettings an object contains the settings of the server proxy.
     * @param threadsCount  the number of threads required to do the download process. If it is 1 it will be serial.
     * @param dir           the directory where to save the downloaded media.
     * @param expectedMediaFormat
     * @return a hash list of the each URL and its path concatenated with the downloaded status.
     */
    public HashMap<String, String> downloadMedia(List<String> mediaURLs, BA_Proxy proxySettings, int threadsCount, String dir, List<String> expectedMediaFormat) {
        HashMap<String, String> resultList = new HashMap<String, String>();
        Downloader downloader = new Downloader(proxySettings);

        if (threadsCount > 1) {
            logger.debug("Downloading Media Using Parallel Threading");
            // Parallel Download
            int size = ((mediaURLs.size() % threadsCount) == 0) ? (mediaURLs.size() / threadsCount) : (mediaURLs.size() % threadsCount) + 1;
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);

            int start = 0;
            int end = size;
            ArrayList<Future> resultedThreadsList = new ArrayList<Future>();
            for (int i = 0; i < threadsCount; i++) {
                resultedThreadsList.add(executeDownloadThread(threadPoolExecutor, mediaURLs, downloader, start, end, dir, expectedMediaFormat));
                start = end;
                end += size;
                if (end > mediaURLs.size())
                    end = mediaURLs.size();
            }

            resultedThreadsList.stream().forEach(result -> {
                try {
                    resultList.putAll((HashMap<String, String>) result.get());
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    logger.error("Error in Download Thread");
                    logger.error("InterruptedException");
                    logger.error("Stack Trace: ", e);
                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                    logger.error("org.bibalex.eol.archiver.services.ArchivesService.downloadMedia: Error during thread downloading. " + e.getMessage());
                    logger.error("Error in Download Thread");
                    logger.error("ExecutionException");
                    logger.error("Stack Trace: ", e);
                }
            });

            // wait for all of the executor threads to finish
            threadPoolExecutor.shutdown();
        } else {
            // sequential Download
            mediaURLs.stream().forEach(url -> {
                resultList.put(url, downloader.downloadFromUrl(url, dir, expectedMediaFormat.get(mediaURLs.indexOf(url))));
            });
        }
        return resultList;

    }

    /**
     * Downlaods a subset of the input URLs list.
     *
     * @param threadPoolExecutor a pool that contain all the threads.
     * @param mediaURLs          the URLs to be downloaded.
     * @param downloader         the downloader object.
     * @param start              the start index in the URLs list to start download from.
     * @param end                the end index in the URLs list to stop download at.
     * @param dir                the directory to save the URLs downloaded files.
     * @return a future result of the path and download status.
     */
    private Future executeDownloadThread(ThreadPoolExecutor threadPoolExecutor, List<String> mediaURLs, Downloader downloader, int start, int end, String dir, List<String> expectedMediaFormat) {
        Future threadResult = threadPoolExecutor.submit(new Callable<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> call() throws Exception {
                HashMap<String, String> result = new HashMap<String, String>();
                mediaURLs.subList(start, end).stream().forEach(url -> {
                    result.put(url, downloader.downloadFromUrl(url, dir, expectedMediaFormat.get(mediaURLs.indexOf(url))));
                });
                return result;
            }
        });
        return threadResult;
    }

    /**
     * Save the uploaded logo of the content partner.
     *
     * @param uploadedFile the logo file.
     * @param cpPath       the path of the logos on the storage layer disk.
     * @param cpId         the content partner id.
     * @return true if successfully uplaoded, false otherwise.
     */
    public boolean saveUploadedLogo(MultipartFile uploadedFile, String cpPath, String cpId) {
        try {
            boolean succeeded = true;
            String fullPath = cpPath + File.separator + cpId + File.separator + uploadedFile.getOriginalFilename();
            Path filePath = Paths.get(fullPath);
            logger.info("Attempting to Save Logo for Content Partner: " + cpId + " to Path: " + fullPath);

            if (Files.notExists(Paths.get(cpPath + File.separator + cpId)))
                Files.createDirectories(Paths.get(cpPath + File.separator + cpId));
            else {
                File dir = new File(cpPath + File.separator + cpId);
                File[] files = dir.listFiles();

                // If there is another logo file
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (!file.delete()) {
                            logger.error("Found Logo for Content Partner: " + cpId + ", Could Not Save New Logo");
                            succeeded = false;
                        } else {
                            logger.info("Deleted Already Existing Logo for Content Partner: " + cpId);
                            logger.info("New Logo for Content Partner: " + cpId + "Successfully Uploaded");
                        }
                    }
                }
            }
            Files.copy(uploadedFile.getInputStream(), filePath);
            return succeeded;
        } catch (IOException e) {
            logger.error("IOException: Failed to Save File: " + uploadedFile.getOriginalFilename());
            logger.error("Stack Trace: ", e);
            return false;
        }
    }

    /**
     * Loads the content partner logo.
     *
     * @param contentPPath the path of the content partners media
     * @param cpId         the content partner id
     * @return the logo file
     */
    public File getCpLogo(String contentPPath, String cpId) {
        if (Files.exists(Paths.get(contentPPath + File.separator + cpId))) {
            File directory = new File(contentPPath + File.separator + cpId);
            File[] listOfFiles = directory.listFiles();
            if (listOfFiles != null && listOfFiles.length > 0) {
                return listOfFiles[0];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
