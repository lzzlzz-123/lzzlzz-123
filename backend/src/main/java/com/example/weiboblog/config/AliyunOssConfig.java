package com.example.weiboblog.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AliyunOssProperties.class)
@ConditionalOnProperty(prefix = "app.aliyun.oss", name = "endpoint")
@Slf4j
public class AliyunOssConfig {

    @Bean
    public OSS ossClient(AliyunOssProperties properties) {
        log.info("Initializing Aliyun OSS client with endpoint: {}", properties.getEndpoint());
        return new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
    }
}