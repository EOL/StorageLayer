package org.bibalex.eol.archiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * Created by maha.mostafa on 4/18/17.
 */
@SpringBootApplication
public class ArchiverApp extends SpringBootServletInitializer {
//    public class ArchiverApp {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ArchiverApp.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ArchiverApp.class, args);
    }
}
