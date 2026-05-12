package com.example.uploadprojectservice;

import com.example.uploadprojectservice.Service.project.config.FileStorageProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		FileStorageProperties.class
})
@OpenAPIDefinition(
		info = @Info(title = "Upload Project", version = "1.0.0"),
		servers = {@Server(url = "http://localhost:8020"), @Server(url = "http://locahost:8005/api/upload-project-service")},
		tags = {@Tag(name = "upload-project-controller", description = "This is the Service Upload-Project description")}
)
public class UploadProjectServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadProjectServiceApplication.class, args);
	}

}
