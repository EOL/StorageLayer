package org.bibalex.eol.imageResizing.controllers;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MimeTypeException;
import org.bibalex.eol.imageResizing.handlers.LogHandler;
import org.bibalex.eol.imageResizing.handlers.MysqlHandler;
import org.bibalex.eol.imageResizing.handlers.ResourceHandler;
import org.bibalex.eol.imageResizing.utils.Constants;
import org.gm4java.engine.GMException;
import org.gm4java.engine.GMService;
import org.gm4java.engine.GMServiceException;
import org.gm4java.engine.support.GMConnectionPoolConfig;
import org.gm4java.engine.support.PooledGMService;
import org.gm4java.engine.support.SimpleGMService;
import org.gm4java.im4java.GMBatchCommand;
import org.im4java.core.*;
import org.im4java.process.ProcessExecutor;
import org.im4java.process.ProcessTask;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

public class ImageResizer {
    private Logger logger;
    private int success, failed ;
    private long filesSize;
    private String sizes ;
    private PooledGMService service;
    GMConnectionPoolConfig config;
//    MysqlHandler mysqlHandler;
//    GMService service;
    GMBatchCommand command;


    public ImageResizer(){
        logger = LogHandler.getLogger(ImageResizer.class.getName());
//        mysqlHandler = new MysqlHandler();
//        service = new SimpleGMService();
         config = new GMConnectionPoolConfig();
//        config.setMaxActive(7);
        service = new PooledGMService(config);
        command = new GMBatchCommand(service, "convert");
        success = 0;
        failed = 0;
        filesSize = 0;
        sizes = null;

    }

    public void imageProcessing(){
//        mysqlHandler.connectToMysql();
        long startTime = System.nanoTime();

        File testDirectory = new File( ResourceHandler.getPropertyValue("source_image_directory"));
        for (File file : testDirectory.listFiles()) {
            sizes = null;
            ConfigurableMimeFileTypeMap mimeType = new ConfigurableMimeFileTypeMap();
            mimeType.setMappings(Constants.missing_formats);

            /*mimeType from apachi tika library output is type/extension eg. image/png
             convert image then resize*/
            logger.info("file mimeType: "+mimeType.getContentType(file).toString());
            if (mimeType.getContentType(file).toString().contains("image")) {

//                convert_resize(file);

//                convertImage(file);
//                resizeAllSizes(file);

                convertImage_using_batch(file);
                resizeAllSizes_using_batch(file);



                filesSize += file.length();
                logger.info("===============================================\n");
            }
        }


        logger.info("total converted and resized images: " + success + " images out of " + (success + failed) + " images in " + (System.nanoTime() - startTime) / 1000000000 + " seconds\nTotal Size: " + filesSize / (1024.0 * 1024.0) + " MB");
//        mysqlHandler.closeMysqlConnection();

    }

    private void convertImage(File file) {
        String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory");

        File outputDirectory = new File(outputDirectoryPath);
        if (!(outputDirectory.exists()))
            outputDirectory.mkdir();

        File converted_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + "jpg");
        try {
            service.execute("convert", file.getPath(), converted_image.getPath());
        } catch (IOException e) {
            failed ++;
            e.printStackTrace();
        } catch (GMException e) {
            failed ++;
            e.printStackTrace();
        } catch (GMServiceException e) {
            failed ++;
            e.printStackTrace();
        }

        logger.info("Image Converted--> source: " + file.getPath() + ", destination: " + converted_image.getPath());
        success ++;

    }

    public void resizeAllSizes(File file)
    {
        resizeImage(file.getPath(), Constants.small_image_square_crop, Constants.small_image_square_crop);
        resizeImage(file.getPath(), Constants.medium_image_square_crop, Constants.medium_image_square_crop);
        resizeImage(file.getPath(), Constants.large_size_x, Constants.large_size_y);
        resizeImage(file.getPath(), Constants.medium_size_x, Constants.medium_size_y);
        resizeImage(file.getPath(), Constants.small_size_x, Constants.small_size_y);
        if (sizes != null && sizes.length() > 0 && sizes.charAt(sizes.length() - 1) == ',') {
            sizes = sizes.substring(0, sizes.length() - 1);
        }

//        String[] splitArray = file.getPath().split("/");
//        int size =  splitArray.length;
//        mysqlHandler.updateTableMedia(Integer.valueOf(splitArray[size-3]),file.getPath(),sizes);
    }

    public void resizeImage(String imagePath, int dimension1, int dimension2) {
        try {
            String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory"),
                    imageName = new File(imagePath).getName();
            File outputDirectory = new File(outputDirectoryPath);
            if (!(outputDirectory.exists()))
                outputDirectory.mkdir();

            File converted_resized_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(imageName) + "."+dimension1+"X"+dimension2+"."
                    + "jpg");
            service.execute("convert", imagePath, "-resize",
                    dimension1+"x"+dimension2,converted_resized_image.getPath());

            logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + converted_resized_image.getPath());

           sizes = sizes == null?  String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",": sizes + String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",";
            success++;
        } catch (IOException e) {
            failed++;
            e.printStackTrace();
        } catch (GMException e) {
            failed++;
            e.printStackTrace();
        } catch (GMServiceException e) {
            failed++;
            e.printStackTrace();
        }
    }

 public void convert_resize(File file) {
     String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory");

     File outputDirectory = new File(outputDirectoryPath);
     if (!(outputDirectory.exists()))
         outputDirectory.mkdir();

     try {
         String imagePath = file.getPath();
         File image_converted = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + "jpg");
         service.execute("convert", file.getPath(), image_converted.getPath());
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_converted.getPath());
         success ++;
         File image_resized_1 = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "."+Constants.small_image_square_crop+"X"+Constants.small_image_square_crop+"."
                 + "jpg");
         service.execute("convert", file.getPath(), "-resize",
                 Constants.small_image_square_crop+"x"+Constants.small_image_square_crop,image_resized_1.getPath() );
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_resized_1.getPath());
         success ++;
         File image_resized_2 = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "."+Constants.medium_image_square_crop+"X"+Constants.medium_image_square_crop+"."
                 + "jpg");
                 service.execute("convert", file.getPath(), "-resize",
                 Constants.medium_image_square_crop+"x"+Constants.medium_image_square_crop,image_resized_2.getPath());
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_resized_2.getPath());
         success ++;
         File image_resized_3 = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + Constants.large_size_x + "X" + Constants.large_size_y + "."
                 + "jpg");
                 service.execute("convert", file.getPath(), "-resize",
                 Constants.large_size_x + "x" + Constants.large_size_y,image_resized_3.getPath() );
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_resized_3.getPath());
         success ++;
         File image_resized_4 = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + Constants.medium_size_x + "X" + Constants.medium_size_y + "."
                 + "jpg");
                 service.execute("convert", file.getPath(), "-resize",
                 Constants.medium_size_x + "x" + Constants.medium_size_y, image_resized_4.getPath());
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_resized_4.getPath());
         success ++;
         File image_resized_5 = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + Constants.small_size_x + "X" + Constants.small_size_y + "."
                 + "jpg");
                 service.execute("convert", file.getPath(), "-resize",
                 Constants.small_size_x + "x" + Constants.small_size_y, image_resized_5.getPath());
         logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + image_resized_5.getPath());
         success ++;
     } catch (IOException e) {
         failed++;
         e.printStackTrace();
     } catch (GMException e) {
         failed++;
         e.printStackTrace();
     } catch (GMServiceException e) {
         failed++;
         e.printStackTrace();
     }
 }

    public void convertImage_using_batch(File file) {
        String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory");

        File outputDirectory = new File(outputDirectoryPath);
        if (!(outputDirectory.exists()))
            outputDirectory.mkdir();

//        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        File converted_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + "jpg");
        op.addImage(file.getPath());
        op.addImage(converted_image.getPath());
        logger.info("Image Converted--> source: " + file.getPath() + ", destination: " + converted_image.getPath());
        success ++;
        try {
            command.run(op);
        } catch (IOException e) {
            failed ++;
            e.printStackTrace();
        } catch (InterruptedException e) {
            failed ++;
            e.printStackTrace();
        } catch (IM4JavaException e) {
            failed ++;
            e.printStackTrace();
        }
    }

    public void resizeAllSizes_using_batch(File file)
    {
        resizeImage_using_batch(file.getPath(), Constants.small_image_square_crop, Constants.small_image_square_crop);
//        resizeImage_using_batch(file.getPath(), Constants.medium_image_square_crop, Constants.medium_image_square_crop);
        resizeImage_using_batch(file.getPath(), Constants.large_size_x, Constants.large_size_y);
        resizeImage_using_batch(file.getPath(), Constants.medium_size_x, Constants.medium_size_y);
//        resizeImage_using_batch(file.getPath(), Constants.small_size_x, Constants.small_size_y);
        if (sizes != null && sizes.length() > 0 && sizes.charAt(sizes.length() - 1) == ',') {
            sizes = sizes.substring(0, sizes.length() - 1);
        }

//        String[] splitArray = file.getPath().split("/");
//        int size =  splitArray.length;
//        mysqlHandler.updateTableMedia(Integer.valueOf(splitArray[size-3]),file.getPath(),sizes);
    }

    public void resizeImage_using_batch(String imagePath, int dimension1, int dimension2) {
        try {
            String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory"),
                    imageName = new File(imagePath).getName();
            File outputDirectory = new File(outputDirectoryPath);
            if (!(outputDirectory.exists()))
                outputDirectory.mkdir();

//            ConvertCmd cmd = new ConvertCmd();
            IMOperation op = new IMOperation();
            File converted_resized_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(imageName) + "."+dimension1+"X"+dimension2+"."
                    + "jpg");

            op.addImage(imagePath);
            op.resize(dimension1, dimension2);
            op.addImage(converted_resized_image.getPath());
            command.run(op);
            logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + converted_resized_image.getPath());

            sizes = sizes == null?  String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",": sizes + String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",";
            success++;
        } catch (IOException e) {
            failed++;
            e.printStackTrace();
        } catch (InterruptedException e) {
            failed++;
            e.printStackTrace();
        } catch (IM4JavaException e) {
            failed++;
            e.printStackTrace();
        }
    }



}
