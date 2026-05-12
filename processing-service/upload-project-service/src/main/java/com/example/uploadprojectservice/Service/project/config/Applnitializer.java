package com.example.uploadprojectservice.Service.project.config;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;

public class Applnitializer implements WebApplicationInitializer{
    @Override
    public void onStartup(ServletContext servletContext) {
        final AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();

        final ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", new DispatcherServlet(appContext));
        registration.setLoadOnStartup(1);
        registration.addMapping("/");

        File uploadDirectory = new File(System.getProperty("java.io.tmpdir"));
        MultipartConfigElement multipartConfigElement = new  MultipartConfigElement(uploadDirectory.getAbsolutePath(), 100000, 100000 * 2, 100000 / 2);

        registration.setMultipartConfig(multipartConfigElement);
    }

}
