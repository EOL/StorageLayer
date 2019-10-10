package org.bibalex.eol.archiver.controllers;

import model.BA_Proxy;
import org.bibalex.eol.archiver.services.ArchivesService;
import org.bibalex.eol.archiver.Components.PropertiesFile;
import org.bibalex.eol.archiver.utils.Constants;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by maha.mostafa on 4/18/17.
 */
@RestController
@RequestMapping("/archiver")
public class RestAPIController {

    private static final Logger logger = LoggerFactory.getLogger(RestAPIController.class);

    @Autowired
    private ArchivesService service;
    private String basePath;
    private String contentPPath;
    public static String mediaTempPath;
    private PropertiesFile app;
    private BA_Proxy proxy;
    public static long maximumFileSize;
    public static String hostName;
    public static int tcpPortNumber;


    @Autowired
    public void setApp(PropertiesFile app) {
        this.app = app;
    }

    @PostConstruct
    public void init() {
        proxy = new BA_Proxy();
        this.basePath = app.getBasePath();
        this.contentPPath = app.getContentPPath();
        this.mediaTempPath = app.getMediaTempPath();
        this.maximumFileSize = app.getMaximumFileSize();
        this.hostName = app.getHostName();
        this.tcpPortNumber = app.getTCPPortNumber();
        proxy.setProxyExists((app.getProxyExists().equalsIgnoreCase("true")) ? true : false);
        proxy.setPort(app.getPort());
        proxy.setProxy(app.getProxy());
        proxy.setUserName(app.getProxyUserName());
        proxy.setPassword(app.getPassword());
    }

    /**
     * A post function to upload a resource.
     *
     * @param resId        the unique id of the resource.
     * @param uploadedFile the uploaded resource.
     * @param isOrg        is "1" if the resource uploaded from the publishing layer, "0" if it was a DWCA resource.
     * @return a success status if succeeded, error otherwise.
     */
    @RequestMapping(value = "/uploadResource/{resId}/{isOrg}", method = RequestMethod.POST)
    public ResponseEntity<String> uploadResource(@PathVariable("resId") String resId, @RequestParam("file") MultipartFile uploadedFile, @PathVariable("isOrg") String isOrg) {
        // By default upload the original resource
        if (!validResourceType(isOrg))
            isOrg = getDefaultResourceType();
        logger.info("Uploading Resource File: " + uploadedFile.getOriginalFilename());
        logger.info("Uploaded Resource File Type: " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "Original " : "DWCA "));
        if (uploadedFile.isEmpty()) {
            logger.error("Exception: The Uploaded File is Empty");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (service.saveUploadedArchive(uploadedFile, basePath, resId, isOrg)) {
            logger.info("Original Resource Successfully Uploaded");
            return new ResponseEntity("Successfully uploaded original resource - " +
                    uploadedFile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
        } else {
            logger.error("Exception: Internal Server Error");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getDefaultResourceType() {
        return Constants.DEFAULT_RESOURCE_TYPE;
    }

    private boolean validResourceType(String isOrg) {
        if (isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) || isOrg.equalsIgnoreCase(Constants.DWCA_RESOURCE_TYPE))
            return true;
        return false;
    }

    /**
     * A post function downloads the original resource or the DWCA resource based on the input type.
     * If the core doesn't exist it will download the original.
     *
     * @param resId the unique id of the resource.
     * @param isOrg is "1" if the resource uploaded from the publishing layer, "0" if it was a DWCA resource.
     * @return the reqired resource file.
     */

    @RequestMapping(value = "/downloadResource/{resId}/{isOrg}/{isNew}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadResource(@PathVariable("resId") String resId, @PathVariable("isOrg") String isOrg, @PathVariable("isNew") String isNew) {
        try {
            // By default upload the original resource
            if (!validResourceType(isOrg))
                isOrg = getDefaultResourceType();

            logger.info("Downloading Resource: " + resId);
            logger.info("Downloaded Resource File Type: " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "Original " : "DWCA "));
//            File file = service.getResourceFile(basePath, resId, isOrg);
            File file = service.getResourceFile(basePath, resId, isOrg, isNew);
            if (file == null)
                return (new ResponseEntity<>(HttpStatus.NOT_FOUND));

            InputStreamResource resource;
            // or use resource byte array
//            Path path = Paths.get(file.getAbsolutePath());
//            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            resource = new InputStreamResource(new FileInputStream(file));

            // prevent caching
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            if (file.getName().startsWith(Constants.ORG_START))
                headers.add("Content-disposition", "inline;filename=" + file.getName().substring(Constants.ORG_START.length() + 1).replaceAll(" ", "_"));
            else
                headers.add("Content-disposition", "inline;filename=" + file.getName().substring(Constants.CORE_START.length() + 1).replaceAll(" ", "_"));

            // uncomment if want the file as attachment
//          headers.add("Content-disposition", "attachment;filename=" + file.getName().substring(4));
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(
                            MediaType.parseMediaType("application/octet-stream"))
                    // uncomment if know the type of the resource and will open it in the browser & keep it inline content type too
//                            .contentType(
//                                    MediaType.parseMediaType("text/html"))
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (FileNotFoundException ex)

        {
            logger.error("HTTP Status: " + HttpStatus.NOT_FOUND);
            logger.error("FileNotFoundException");
            logger.error("Stack Trace: ", ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e)

        {
            logger.error("HTTP Status: " + HttpStatus.INTERNAL_SERVER_ERROR);
            logger.error("IOException");
            logger.error("Stack Trace: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * A post function that dowloads a list of media URLs files.
     *
     * @param resId     the resource unique id.
     * @param mediaURLs a list of the media URLsFailed to resolve argument 2 of type 'java.util.List'in the format of "["url1", "url2", ..]"
     *                  //     * @param expectedMediaFormat a list of provided media urls expected content type
     * @return a hash in json format of each URL and its path in storage layer concatenated with the download status.
     */
    @RequestMapping(value = "/downloadMedia/{resId}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<HashMap<String, String>> downloadMedia(@PathVariable("resId") String resId,
                                                                 @RequestBody List<List<String>> mediaURLs) {

        // TODO
        // uses UriComponentsBuilder, rest template to generate client
        logger.info("Downloading Media of Resource: " + resId);

        long startT = System.currentTimeMillis();
        HashMap<String, String> resultList = new HashMap<String, String>();

        try {
            ArrayList<String> expectedFormatList = new ArrayList<String>(),
                    mediaURLS = new ArrayList<>();
            for (int i = 0; i < mediaURLs.size(); i++) {
                String url = mediaURLs.get(i).get(0),
                        expectedFormat = mediaURLs.get(i).get(1);
                expectedFormatList.add(expectedFormat);
                mediaURLS.add(url);
//                System.out.println("----------- Media URL: " + url + "---------------");
                logger.debug("Media URL: " + url);
            }


            String downloadMediaPath = basePath + File.separator + resId + File.separator + Constants.MEDIA_FOLDER + File.separator;
            Path mediaPath = Paths.get(downloadMediaPath);

            if (Files.notExists(mediaPath)) {
                Files.createDirectories(mediaPath);
            }
            resultList = service.downloadMedia(mediaURLS, proxy, app.getThreadsCount(), downloadMediaPath, expectedFormatList);
            logger.info("Downloaded Media of Resource: " + resId + " in " + (System.currentTimeMillis() - startT) + " milliseconds");

            logger.debug("Downloaded URLS:'");
            resultList.forEach((k, v) -> {
                logger.debug("URL: (" + k + ") --> (" + v + ")");
            });
            if (resultList.size() == 0) {
//                logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.downloadMedia(): error during download threads.");
                logger.error("HTTP Status: " + HttpStatus.INTERNAL_SERVER_ERROR);
                logger.error("Error in Download Threads");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // prevent caching
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(
                            MediaType.parseMediaType("application/json"))
                    .body(resultList);
        } catch (IOException e) {
            logger.error("HTTP Status: " + HttpStatus.INTERNAL_SERVER_ERROR);
            logger.error("IOException");
            logger.error("Stack Trace: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * A post function to upload a content partner logo.
     *
     * @param cpId         the unique id of the content partner.
     * @param uploadedFile the uploaded resource.
     * @return a success status if succeeded, error otherwise.
     */
    @RequestMapping(value = "/uploadCpLogo/{cpId}", method = RequestMethod.POST)
    public ResponseEntity<String> uploadCpLogo(@PathVariable("cpId") String cpId, @RequestParam("logo") MultipartFile uploadedFile) {
        // By default upload the original resource

//        logger.info("Uploading Logo File: " + uploadedFile.getOriginalFilename() + "] of cp [" + cpId + "]");
        logger.info("Uploading Logo for Content Partner: " + cpId);
        logger.info("Logo File Path: " + uploadedFile.getOriginalFilename());
        if (uploadedFile.isEmpty()) {
            logger.error("Uploaded File is Empty");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (service.saveUploadedLogo(uploadedFile, contentPPath, cpId))
        {
            logger.info("Successfully Uploaded Logo File");
            return new ResponseEntity("Successfully uploaded logo file - " +
                    uploadedFile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
        }
        else {
            logger.error("HTTP Status: " + HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * A post function downloads logo of the content partner.
     *
     * @param cpId the unique id of the content partner.
     * @return the reqired logo file.
     */
    @RequestMapping(value = "/downloadCpLogo/{cpId}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadCpLogo(@PathVariable("cpId") String cpId) {
        try {
            logger.info("Downloading Logo for Content Partner: " + cpId);
            File logo = service.getCpLogo(contentPPath, cpId);
            InputStreamResource resource;
            // or use resource byte array
//            Path path = Paths.get(file.getAbsolutePath());
//            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            if (logo != null) {
                resource = new InputStreamResource(new FileInputStream(logo));
            } else {
                logger.error("Logo Not Found");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // uncomment if want the file as attachment
//          headers.add("Content-disposition", "attachment;filename=" + file.getName().substring(4));
            return ResponseEntity
                    .ok()
                    .headers(getHeaders(logo))
                    .contentLength(logo.length())
                    .contentType(
                            MediaType.parseMediaType("application/octet-stream"))
                    // uncomment if know the type of the resource and will open it in the browser & keep it inline content type too
//                            .contentType(
//                                    MediaType.parseMediaType("text/html"))
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (FileNotFoundException ex) {
            logger.error("HTTP Status: " + HttpStatus.NOT_FOUND);
            logger.error("FileNotFoundException");
            logger.error("Stack Trace: ", ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            logger.error("HTTP Status: " + HttpStatus.NOT_FOUND);
            logger.error("IOException");
            logger.error("Stack Trace: ", e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private HttpHeaders getHeaders(File file) {
        // prevent caching
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Content-disposition", "inline;filename=" + file.getName());

        return headers;
    }


}
