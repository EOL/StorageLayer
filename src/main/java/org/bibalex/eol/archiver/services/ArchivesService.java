package org.bibalex.eol.archiver.services;

/**
 * Created by hduser on 4/18/17.
 */

import model.BA_Proxy;
import org.bibalex.eol.archiver.controllers.RestAPIController;
import org.bibalex.eol.archiver.utils.Constants;
import org.bibalex.eol.archiver.utils.Downloader;
import org.bibalex.eol.archiver.utils.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     *
     * @param uploadedFile
     * @param basePath
     * @param resId
     * @throws IOException
     */
    public boolean saveUploadedArchive(MultipartFile uploadedFile, String basePath, String resId, String isOrg) {
        try {
            boolean succeeded = true;
            String prefix = isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) ? Constants.ORG_START : Constants.CORE_START;

            String fullPath = basePath + File.separator + resId + File.separator + prefix + "_" + uploadedFile.getOriginalFilename();

            logger.debug("org.bibalex.eol.archiver.services.ArchivesService.saveUploadedArchive: fullPath: " + fullPath);
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
                        logger.error("org.bibalex.eol.archiver.services.ArchivesService.saveUploadedArchive(): Resource (" + resId + ") already exists, it can't be deleted.");
                        succeeded = false;
                    } else {
                        Files.copy(uploadedFile.getInputStream(), filePath);
                        logger.info("Resource (" + resId + ") already exists, it will be replaced.");
                    }
                } else {
                    logger.debug("Resource (" + resId + ") is created for first time.");
                    Files.copy(uploadedFile.getInputStream(), filePath);
                }
            }
            return succeeded;
        } catch (IOException e) {
            logger.error("org.bibalex.eol.archiver.services.ArchivesService.saveUploadedArchive(): Failed to save file (" + uploadedFile.getOriginalFilename() +")"+ e.getMessage());
            return false;
        }
    }


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
            logger.info("Downloading to harvester core file [" + file.getName() + "] of resource [" + resId + "] ..");
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
                if (isOrg.equalsIgnoreCase("1"))
                    logger.info("Downloading to harvester original file [" + file.getName() + "] of resource [" + resId + "] ..");
                else {
                    logger.info("Downloading to harvester core file [" + file.getName() + "] of resource [" + resId + "] .. But it doesn't exist so original is downloaded.");
                    // uncomment if want to not download original when core is required
//                    succeeded = false;
                }
            }
        }
        return file;
    }


    /**
     * Copy file from source to destination using stream
     * @param source
     * @param dest
     * @throws IOException
     */
    public void copyFile(File source, File dest) throws IOException {
        fileManager.customBufferBufferedStreamCopy(source, dest);
    }

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
                System.out.println("URL: " + url);
                resultList.put(url, downloader.downloadFromUrl(url, dir));
            });
        }
        return resultList;

    }

    private Future executeDownloadThread(ThreadPoolExecutor threadPoolExecutor, List<String> mediaURLs, Downloader downloader, int start, int end, String dir) {
        Future threadResult = threadPoolExecutor.submit(new Callable<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> call() throws Exception {
                HashMap<String, String> result = new HashMap<String, String>();
                System.out.println("Thread start from index----------------->" + start);
                mediaURLs.subList(start, end).stream().forEach(url -> {
                   result.put(url, downloader.downloadFromUrl(url, dir));
                });
                return result;
            }
        });
        return threadResult;
    }
}
