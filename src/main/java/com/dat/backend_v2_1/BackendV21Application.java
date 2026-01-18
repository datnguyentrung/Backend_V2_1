package com.dat.backend_v2_1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class BackendV21Application {

    public static void main(String[] args) {
        // Thêm dòng này ở ngay đầu hàm main
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SpringApplication.run(BackendV21Application.class, args);
    }

}
