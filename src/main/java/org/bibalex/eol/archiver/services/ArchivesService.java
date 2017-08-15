package org.bibalex.eol.archiver.services;

/**
 * Created by maha.mostafa on 4/18/17.
 */

import model.BA_Proxy;
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
import java.util.concurrent.*;

@Service
public class ArchivesService {

    private static final Logger logger = LoggerFactory.getLogger(ArchivesService.class);
    private FileManager fileManager;


    public ArchivesService() {
        this.fileManager = new FileManager();
        logger.debug("ArchivesService testing init");
    }

    /**
     * Saves the input resource file on disk, if it was found before, it will be replaced.
     * @param uploadedFile the uploaded file.
     * @param basePath the directory of where to save it.
     * @param resId the resource id of the uploaded file.
     * @param isOrg 1 if it is the uploaded file, 0 if it is DWCA.
     * @return true if the saving of the resource in its right directory was successful
     */
    public boolean saveUploadedArchive(MultipartFile uploadedFile, String basePath, String resId, String isOrg) {
        try {
            boolean succeeded = true;
            String prefix = isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) ? Constants.ORG_START : Constants.CORE_START;

            String fullPath = basePath + File.separator + resId + File.separator + prefix + "_" + uploadedFile.getOriginalFilename();

            Path directoryPath = Paths.get(basePath + File.separator + resId);
            Path filePath = Paths.get(fullPath);

            if(Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
                Files.copy(uploadedFile.getInputStream(), filePath);
            } else {
                File dir = new File(basePath + File.separator + resId);
                File[] files = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith(prefix + "_");
                    }
                });

                // If there is another org file
                if(files.length > 0) {
                    File file = files[0];
                    if (!file.delete()) {
                        logger.error("org.bibalex.eol.archiver.services.ArchivesService.saveUploadedArchive(): Resource (" + resId + ") is " +
                                ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + " and already exists, it can't be deleted.");
                        succeeded = false;
                    } else {
                        Files.copy(uploadedFile.getInputStream(), filePath);
                        logger.info("Resource (" + resId + ") is " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + "and already exists, it will be replaced.");
                    }
                } else {
                    logger.info("Resource (" + resId + ") is " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + " and created for first time.");
                    Files.copy(uploadedFile.getInputStream(), filePath);
                }
            }
            return succeeded;
        } catch (IOException e) {
            logger.error("org.bibalex.eol.archiver.services.ArchivesService.saveUploadedArchive(): Failed to save file (" + uploadedFile.getOriginalFilename() +") is "
                    + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + e.getMessage());
            return false;
        }
    }

    /**
     * Reads the resource file from its directory and returns it. If the DWCA doesn't exist so it will return the publishing uploaded file.
     * @param basePath the base directory where all resources are saved.
     * @param resId the id of the required resource.
     * @param isOrg 1 if the required file is the uploaded in the publishing layer, 0 if the required file is the DWCA
     * @return the required resource file.
     */
    public File getResourceFile(String basePath, String resId, String isOrg) {
        File file = null;
        File dir = new File(basePath + File.separator + resId);
        File[] orgFiles, coreFiles;
        coreFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(Constants.CORE_START + "_");
            }
        });
        if (!isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) && coreFiles.length != 0) {
            // Core file
            file = coreFiles[0];
            logger.info("Downloading DWCA file [" + file.getName() + "] of resource [" + resId + "] ..");
        } else {
            orgFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(Constants.ORG_START + "_");
                }
            });

            if (orgFiles.length == 0) {
                logger.error("org.bibalex.eol.archiver.services.ArchivesService.getResourceFile(): No org resource to download.");
            } else {
                file = orgFiles[0];
                // Input stream resource
                if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE))
                    logger.info("Downloading original file [" + file.getName() + "] of resource [" + resId + "] ..");
                else {
                    logger.info("Downloading DWCA file [" + file.getName() + "] of resource [" + resId + "] .. But it doesn't exist so original will be downloaded.");
                    // uncomment if want to not download original when core is required
//                    file = null;
                }
            }
        }
        return file;
    }


    /**
     * Copy file from source to destination using stream.
     * @param source the source file to be copied.
     * @param dest the destination file to copy the data to.
     * @throws IOException
     */
    public void copyFile(File source, File dest) throws IOException {
        fileManager.customBufferBufferedStreamCopy(source, dest);
    }

    /**
     * Downloads the input media urls and saves them on disk.
     * @param mediaURLs a list of the URLs to be downloaded.
     * @param proxySettings an object contains the settings of the server proxy.
     * @param threadsCount the number of threads required to do the download process. If it is 1 it will be serial.
     * @param dir the dircetory where to save the downloaded media.
     * @return a hash list of the each URL and its path concatenated with the downloaded status.
     */
    public HashMap<String, String> downloadMedia(List<String> mediaURLs, BA_Proxy proxySettings, int threadsCount, String dir)  {
        HashMap<String, String> resultList = new HashMap<String, String>();
        Downloader downloader = new Downloader(proxySettings);

        if(threadsCount > 1) {
            logger.info("Downloading using parallel threading.");
            // Parallel Download
            int size = ((mediaURLs.size() % threadsCount) == 0) ? (mediaURLs.size() / threadsCount) : (mediaURLs.size() % threadsCount) + 1;
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);

            int start = 0;
            int end = size;
            ArrayList<Future> resultedThreadsList = new ArrayList<Future>();
            for(int i = 0; i < threadsCount; i++) {
                resultedThreadsList.add(executeDownloadThread(threadPoolExecutor, mediaURLs, downloader, start, end, dir));
                start = end;
                end += size;
                if(end > mediaURLs.size())
                    end = mediaURLs.size();
            }

            resultedThreadsList.stream().forEach(result ->  {
                try {
                    resultList.putAll((HashMap<String, String>) result.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.error("org.bibalex.eol.archiver.services.ArchivesService.downloadMedia: Error during thread downloading: " + e.getMessage());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    logger.error("org.bibalex.eol.archiver.services.ArchivesService.downloadMedia: Error during thread downloading. " + e.getMessage());
                }
            });

            // wait for all of the executor threads to finish
            threadPoolExecutor.shutdown();
        } else {
            // sequential Download
            mediaURLs.stream().forEach(url -> {
                resultList.put(url, downloader.downloadFromUrl(url, dir));
            });
        }
        return resultList;

    }

    /**
     * Downlaods a subset of the input URLs list.
     * @param threadPoolExecutor a pool that contain all the threads.
     * @param mediaURLs the URLs to be downloaded.
     * @param downloader the downloader object.
     * @param start the start index in the URLs list to start download from.
     * @param end the end index in the URLs list to stop download at.
     * @param dir the directory to save the URLs downloaded files.
     * @return a future result of the path and download status.
     */
    private Future executeDownloadThread(ThreadPoolExecutor threadPoolExecutor, List<String> mediaURLs, Downloader downloader, int start, int end, String dir) {
        Future threadResult = threadPoolExecutor.submit(new Callable<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> call() throws Exception {
                HashMap<String, String> result = new HashMap<String, String>();
                mediaURLs.subList(start, end).stream().forEach(url -> {
                   result.put(url, downloader.downloadFromUrl(url, dir));
                });
                return result;
            }
        });
        return threadResult;
    }
}
