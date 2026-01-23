package com.uloaix.xiaolu_aicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.uloaix.xiaolu_aicode.mapper")
public class XiaoluAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoluAiCodeApplication.class, args);
    }

}
