package org.bibalex.eol.archiver.utils;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageResizer {
    private static final Logger logger = LoggerFactory.getLogger(ImageResizer.class);

    public static void main(String[] args) throws IOException, InterruptedException {
    }

    void resizeImage(String imagePath, int imageSize, String formatName) throws IOException {
        File image = new File(imagePath);
        BufferedImage img = ImageIO.read(image),
                thumbnail = Scalr.resize(img, imageSize);
        ImageIO.write(thumbnail, formatName.substring(1), new File(imagePath + "_thumbnail" + formatName));
        logger.info("Thumbnail Icon Created");
    }

    void createThumbnail(String imagePath, String formatName) throws IOException {
        resizeImage(imagePath, 88, formatName);
        System.out.println("Format:" + formatName);
    }
}
