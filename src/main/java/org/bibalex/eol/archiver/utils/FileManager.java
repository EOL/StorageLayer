package org.bibalex.eol.archiver.utils;

import org.bibalex.eol.archiver.services.ArchivesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by maha.mostafa on 5/15/17.
 */
public class FileManager {

    private static final int BUFFER = 8192;
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    public void customBufferBufferedStreamCopy(File source, File target) {
        try {
            InputStream fis = null;
            OutputStream fos = null;
            fis = new BufferedInputStream(new FileInputStream(source));
            fos = new BufferedOutputStream(new FileOutputStream(target));

            byte[] buf = new byte[BUFFER];

            int i;

            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
            close(fis);
            close(fos);
        } catch (IOException e) {
//            e.printStackTrace();
            logger.error("IOException: ", e);
        }

    }

    public void nioBufferCopy(File source, File target) {
        FileChannel in = null;
        FileChannel out = null;

        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(target).getChannel();

            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
            while (in.read(buffer) != -1) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    out.write(buffer);
                }

                buffer.clear();
            }
        } catch (IOException e) {
//            e.printStackTrace();
            logger.error("IOException: ", e);
        } finally {
            close(in);
            close(out);
        }
    }

    public void nioTransferCopy(File source, File target) {
        FileChannel in = null;
        FileChannel out = null;

        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(target).getChannel();

            long size = in.size();
            long transferred = in.transferTo(0, size, out);

            while (transferred != size) {
                transferred += in.transferTo(transferred, size - transferred, out);
            }
        } catch (IOException e) {
//            e.printStackTrace();
            logger.error("IOException: ", e);
        } finally {
            close(in);
            close(out);
        }
    }

    public void copyFileUsingJava7Files(File source, File dest) {
        try {
            Files.copy(source.toPath(), dest.toPath());
        } catch (IOException e) {
            logger.error("IOException: ", e);
        }
    }

    public void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
//                e.printStackTrace();
                logger.error("IOException: ", e);
            }
        }
    }

    public void saveFile(MultipartFile file, String path) {
        try {
            byte[] bytes = new byte[0];
            bytes = file.getBytes();
            Path fPath = Paths.get(path + file.getOriginalFilename());
            Files.write(fPath, bytes);
        } catch (IOException e) {
            logger.error("IOException: ", e);

        }
    }
}
