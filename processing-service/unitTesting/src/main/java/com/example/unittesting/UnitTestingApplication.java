package com.example.unittesting;

import core.z3.Z3NativeLoader;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableEurekaClient

@OpenAPIDefinition(
        info = @Info(title = "Unit-Testing", version = "1.0.0"),
        servers = {@Server(url = "http://localhost:8006"), @Server(url = "http://locahost:8005/api/unit-testing-service")},
        tags = {@Tag(name = "unit-testing-controller", description = "This is the Service Unit-Testing description")}
)
public class UnitTestingApplication {

    static {
        Z3NativeLoader.load();
    }

    private static ConfigurableApplicationContext context;
    private static String[] args;

    public static void main(String[] args) {
        UnitTestingApplication.args = args;
        context = SpringApplication.run(UnitTestingApplication.class, args);
        System.out.println(context); // ...
    }

    private static final Object RESTART_LOCK = new Object();
    private static volatile boolean RESTARTING = false;

    public static boolean isRestarting() {
        return RESTARTING;
    }

    public static void restart() {
        synchronized (RESTART_LOCK) {
            // avoid concurrent/overlapping restarts
            if (RESTARTING) {
                return;
            }
            RESTARTING = true;
        }

        Thread thread = new Thread(() -> {
            try {
                long startRunTestTime = System.nanoTime();
                context.close();
                context = SpringApplication.run(UnitTestingApplication.class, args);
                long endRunTestTime = System.nanoTime();

                double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;
                System.out.println("restart time :" + runTestDuration);
            } finally {
                RESTARTING = false;
            }
        });

        thread.setDaemon(false);
        thread.start();
    }


}
