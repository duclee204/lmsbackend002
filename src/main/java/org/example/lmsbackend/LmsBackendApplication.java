package org.example.lmsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class LmsBackendApplication {

    @PostConstruct
    public void init() {
        // 🌏 Set default timezone to Vietnam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("🌏 Default timezone set to: " + TimeZone.getDefault().getID());
    }

    public static void main(String[] args) {
        SpringApplication.run(LmsBackendApplication.class, args);
    }

}
