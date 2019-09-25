package org.bibalex.eol.imageResizing.controllers;

import java.io.*;


import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MimeTypeException;
import org.bibalex.eol.imageResizing.handlers.LogHandler;
import org.bibalex.eol.imageResizing.handlers.MysqlHandler;
import org.bibalex.eol.imageResizing.handlers.ResourceHandler;
import org.bibalex.eol.imageResizing.utils.Constants;
import org.im4java.core.*;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

public class ImageResizer {
    private Logger logger;
    private int success, failed ;
    private long filesSize;
    private String sizes ;
    MysqlHandler mysqlHandler;

    public ImageResizer(){
        logger = LogHandler.getLogger(ImageResizer.class.getName());
        mysqlHandler = new MysqlHandler();
        success = 0;
        failed = 0;
        filesSize = 0;
        sizes = null;
    }

    public void imageProcessing(){
        mysqlHandler.connectToMysql();
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
                convertImage(file);
                resizeAllSizes(file);
                filesSize += file.length();
                logger.info("===============================================\n");
            }
        }
        logger.info("total converted and resized images: " + success + " images out of " + (success + failed) + " images in " + (System.nanoTime() - startTime) / 1000000000 + " seconds\nTotal Size: " + filesSize / (1024.0 * 1024.0) + " MB");
        mysqlHandler.closeMysqlConnection();

    }

    private void convertImage(File file) {
        String outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory");

        File outputDirectory = new File(outputDirectoryPath);
        if (!(outputDirectory.exists()))
            outputDirectory.mkdir();

        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        File converted_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(file.getName()) + "." + "jpg");
        op.addImage(file.getPath());
        op.addImage(converted_image.getPath());
        logger.info("Image Converted--> source: " + file.getPath() + ", destination: " + converted_image.getPath());
        success ++;
        try {
            cmd.run(op);
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
        String[] splitArray = file.getPath().split("/");
        int size =  splitArray.length;
        mysqlHandler.updateTableMedia(Integer.valueOf(splitArray[size-3]),file.getPath(),sizes);
    }

    public void resizeImage(String imagePath, int dimension1, int dimension2) {
        try {
            String extension = FilenameUtils.getExtension(imagePath),
                    outputDirectoryPath = ResourceHandler.getPropertyValue("resize_image_directory"),
                    imageName = new File(imagePath).getName();
            File outputDirectory = new File(outputDirectoryPath);
            if (!(outputDirectory.exists()))
                outputDirectory.mkdir();

            ConvertCmd cmd = new ConvertCmd();
            IMOperation op = new IMOperation();
            File converted_resized_image = new File(outputDirectoryPath + FilenameUtils.removeExtension(imageName) + "."+dimension1+"X"+dimension2+"."
                    + "jpg");
            op.addImage(imagePath);
            op.resize(dimension1, dimension2);
            op.addImage(converted_resized_image.getPath());
            cmd.run(op);
            logger.info("Image Resized and Converted--> source:  "+ imagePath + ", destination: " + converted_resized_image.getPath());

           sizes = sizes == null?  String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",": sizes + String.valueOf(dimension1) + "X" + String.valueOf(dimension2) + ",";
            success++;
        } catch (IOException e) {
            failed++;
            e.printStackTrace();
//            System.out.println(e);
        } catch (InterruptedException e) {
            failed++;
            e.printStackTrace();
//            System.out.println(e);
        } catch (IM4JavaException e) {
            failed++;
            e.printStackTrace();
//            System.out.println(new MimetypesFileTypeMap().getContentType(imagePath));
//            System.out.println(e);
        }
    }



}
