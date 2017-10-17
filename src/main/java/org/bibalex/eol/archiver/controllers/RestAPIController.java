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
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

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
    private PropertiesFile app;
    private BA_Proxy proxy;

    @Autowired
    public void setApp(PropertiesFile app) {
        this.app = app;
    }

    @PostConstruct
    public void init() {
        proxy = new BA_Proxy();
        this.basePath = app.getBasePath();
        this.contentPPath = app.getContentPPath();
        proxy.setProxyExists((app.getProxyExists().equalsIgnoreCase("true")) ? true : false);
        proxy.setPort(app.getPort());
        proxy.setProxy(app.getProxy());
        proxy.setUserName(app.getProxyUserName());
        proxy.setPassword(app.getPassword());
    }

    /**
     * A post function to upload a resource.
     * @param resId the unique id of the resource.
     * @param uploadedFile the uploaded resource.
     * @param isOrg is "1" if the resource uploaded from the publishing layer, "0" if it was a DWCA resource.
     * @return a success status if succeeded, error otherwise.
     */
    @RequestMapping(value="/uploadResource/{resId}/{isOrg}", method = RequestMethod.POST)
    public ResponseEntity<String> uploadResource(@PathVariable("resId") String resId, @RequestParam("file") MultipartFile uploadedFile, @PathVariable("isOrg") String isOrg)  {
        // By default upload the original resource
        if(! validResourceType(isOrg))
            isOrg = getDefaultResourceType();
        logger.info("Uploading resource file [" + uploadedFile.getOriginalFilename() + "] which is " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA ") + " ..");
        if (uploadedFile.isEmpty()) {
            logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.uploadResource: uploaded file is empty.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(service.saveUploadedArchive(uploadedFile, basePath, resId, isOrg))
            return new ResponseEntity("Successfully uploaded original resource - " +
                uploadedFile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
        else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private String getDefaultResourceType() {
        return Constants.DEFAULT_RESOURCE_TYPE;
    }

    private boolean validResourceType(String isOrg) {
        if(isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE) || isOrg.equalsIgnoreCase(Constants.DWCA_RESOURCE_TYPE))
            return true;
        return false;
    }

    /**
     * A post function downloads the original resource or the DWCA resource based on the input type.
     * If the core doesn't exist it will download the original.
     * @param resId the unique id of the resource.
     * @param isOrg is "1" if the resource uploaded from the publishing layer, "0" if it was a DWCA resource.
     * @return the reqired resource file.
     */
    @RequestMapping(value = "/downloadResource/{resId}/{isOrg}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadResource(@PathVariable("resId") String resId, @PathVariable("isOrg") String isOrg) {
        try {
            // By default upload the original resource
            if(! validResourceType(isOrg))
                isOrg = getDefaultResourceType();

            logger.info("Downloading resource file [" + resId + "] which is " + ((isOrg.equalsIgnoreCase(Constants.DEFAULT_RESOURCE_TYPE)) ? "original " : "DWCA "));
            File file = service.getResourceFile(basePath, resId, isOrg);
            InputStreamResource resource;
            // or use resource byte array
//            Path path = Paths.get(file.getAbsolutePath());
//            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            if(file != null) {
                resource = new InputStreamResource(new FileInputStream(file));
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // prevent caching
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            if(file.getName().startsWith(Constants.ORG_START))
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
        } catch(FileNotFoundException ex) {
            logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.downloadResource():" + ex.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.downloadResource():" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }



    // TODO later
    @RequestMapping(value = "/downloadResourceMultiFile", method = RequestMethod.GET)
    public void downloadResourceMultiFile(@RequestParam("resId") String resId, HttpServletResponse response) {

//        File[] files = (new File(basePath + File.separator + resId)).listFiles();
//        // Get the file
//        FileInputStream fis = null;
//        try {
//            fis = new FileInputStream(files[0]);
//
//        } catch (FileNotFoundException fnfe) {
//            // If the file does not exists, continue with the next file
//            logger.debug("Could find file " + files[0].getAbsolutePath());
//        }
//        response.addHeader("Content-disposition", "inline;filename=myfilename.txt");
//        response.setContentType("text/plain");
//
//        // Copy the stream to the response's output stream.
//        try {
//            IOUtils.copy(fis, response.getOutputStream());
//
//        response.flushBuffer();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        ///--------------------------------------------------------
//        System.out.println("-------d-----");
//
//
//        // Set the response type and specify the boundary string
//        response.setContentType("multipart/x-mixed-replace;boundary=END");
//        System.out.println("------------");
//        // Set the content type based on the file type you need to download
//        String contentType = "Content-type: text/rtf";
//
//        // List of files to be downloaded
//        File[] files = (new File(basePath + File.separator + resId)).listFiles();
//
//        ServletOutputStream out = null;
//        try {
//            out = response.getOutputStream();
//
//
//
//        // Print the boundary string
//        out.println();
//        out.println("--END");
//
//        for (File file : files) {
//            System.out.println("FILES --------------- " + file.getName());
//
//            // Get the file
//            FileInputStream fis = null;
//            try {
//                fis = new FileInputStream(file);
//
//            } catch (FileNotFoundException fnfe) {
//                // If the file does not exists, continue with the next file
//                System.out.println("Couldfind file " + file.getAbsolutePath());
//                continue;
//            }
//
//            BufferedInputStream fif = new BufferedInputStream(fis);
//
//            // Print the content type
//            out.println(contentType);
//            out.println("Content-Disposition: attachment; filename=" + file.getName());
//            out.println();
//
//            System.out.println("Sending " + file.getName());
//
//            // Write the contents of the file
//            int data = 0;
//            while ((data = fif.read()) != -1) {
//                out.write(data);
//            }
//            fif.close();
//
//            // Print the boundary string
//            out.println();
//            out.println("--END");
//            out.flush();
//            System.out.println("Finisheding file " + file.getName());
//        }
//
//        // Print the ending boundary string
//        out.println("--END--");
//        out.flush();
//        out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    /**
     *  A post function that dowloads a list of media URLs files.
     * @param resId the resource unique id.
     * @param mediaURLs, a list of the media URLs in the format of "["url1", "url2", ..]"
     * @return a hash in json format of each URL and its path in storage layer concatenated with the download status.
     */
    @RequestMapping(value="/downloadMedia/{resId}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<HashMap<String, String>> downloadMedia(@PathVariable("resId") String resId,
                                             @RequestBody List<String> mediaURLs) {

        // TODO
        // uses UriComponentsBuilder, rest template to generate client
        logger.info("Downloading Media of resource (" + resId + ") ..");

        long startT = System.currentTimeMillis();
        HashMap<String, String> resultList = new HashMap<String, String>();

        try {
            String downloadMediaPath = basePath + File.separator + resId + File.separator + Constants.MEDIA_FOLDER + File.separator;
            Path mediaPth = Paths.get(downloadMediaPath);

            if(Files.notExists(mediaPth)) {
                Files.createDirectories(mediaPth);
            }

            resultList = service.downloadMedia(mediaURLs, proxy, app.getThreadsCount(), downloadMediaPath);
            logger.info("Time consumed for media of resource (" + resId + "):" + (System.currentTimeMillis() - startT) + " ms");

            logger.debug("Downloaded URLS:'");
            resultList.forEach((k,v) -> {
                logger.debug("URL: (" + k + ") --> (" + v + ")");
            });
            if(resultList.size() == 0) {
                logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.downloadMedia(): error during download threads.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
            logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.downloadMedia(): error in creating media folder: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    /**
     * A post function to upload a content partner logo.
     * @param cpId the unique id of the content partner.
     * @param uploadedFile the uploaded resource.
     * @return a success status if succeeded, error otherwise.
     */
    @RequestMapping(value="/uploadContentPartnerLogo/{cpId}", method = RequestMethod.POST)
    public ResponseEntity<String> uploadContentPartnerLogo(@PathVariable("cpId") String cpId, @RequestParam("file") MultipartFile uploadedFile)  {
        // By default upload the original resource

        logger.info("Uploading logo file [" + uploadedFile.getOriginalFilename() + "] of cp [" + cpId + "]");
        if (uploadedFile.isEmpty()) {
            logger.error("org.bibalex.eol.archiver.controllers.RestAPIController.uploadCPlogo: uploaded file is empty.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(service.saveUploadedLogo(uploadedFile, contentPPath, cpId))
            return new ResponseEntity("Successfully uploaded logo file - " +
                    uploadedFile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
        else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

//    /**
//     *  A post function that dowloads a list of media URLs files.
//
//     * @return a hash in json format of each URL and its path in storage layer concatenated with the download status.
//     */
//    @RequestMapping(value="/test", method = RequestMethod.POST)
//    public ResponseEntity<String> test() {
//
//        System.out.println("in --------------------- fun --------------- ");
//
//        return new ResponseEntity<String>("teeeeeeeeeeeeeeeeeeeeeest", HttpStatus.OK);
//
//    }

    }
