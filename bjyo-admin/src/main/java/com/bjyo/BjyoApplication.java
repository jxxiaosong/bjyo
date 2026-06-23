package com.bjyo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author bjyo
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class BjyoApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(BjyoApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  BJYO启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}