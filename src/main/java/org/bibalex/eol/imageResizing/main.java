package org.bibalex.eol.imageResizing;

import org.bibalex.eol.imageResizing.controllers.ImageResizer;
import org.bibalex.eol.imageResizing.handlers.LogHandler;
import org.bibalex.eol.imageResizing.handlers.MysqlHandler;
import org.bibalex.eol.imageResizing.handlers.ResourceHandler;

public class main {
    public static void main(String[] args) {


        ResourceHandler.initialize("configs.properties");
        LogHandler.initializeHandler();
        ImageResizer imageResizer = new ImageResizer();
        imageResizer.imageProcessing();
    }
}
