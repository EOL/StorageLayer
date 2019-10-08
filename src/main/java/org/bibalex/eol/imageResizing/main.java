package org.bibalex.eol.imageResizing;

import org.bibalex.eol.imageResizing.controllers.ImageResizer;
import org.bibalex.eol.imageResizing.handlers.LogHandler;
import org.bibalex.eol.imageResizing.handlers.MysqlHandler;
import org.bibalex.eol.imageResizing.handlers.ResourceHandler;
import org.gm4java.engine.GMService;
import org.gm4java.engine.support.SimpleGMService;
import org.gm4java.im4java.GMBatchCommand;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import java.io.IOException;


public class main {
    public static void main(String[] args) {


        ResourceHandler.initialize("configs.properties");
        LogHandler.initializeHandler();
        ImageResizer imageResizer = new ImageResizer();
        imageResizer.imageProcessing();

//
////          SimpleGMService service = new SimpleGMService();
//        GMService service = new SimpleGMService();
//
//         GMBatchCommand command = new GMBatchCommand(service, "convert");
//        // create the operation, add images and operators/options
//
//             IMOperation op = new IMOperation();
//           op.addImage("/home/ba/Downloads/wikiMedia/test2/148/media/index.tiff");
//            op.resize(800, 600);
//           op.addImage("/home/ba/Downloads/wikiMedia/thumbnails/output.jpg");
//             // execute the operation
//        try {
//            command.run(op);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (IM4JavaException e) {
//            e.printStackTrace();
//        }
   }
}
