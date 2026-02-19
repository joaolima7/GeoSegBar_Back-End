package com.geosegbar.configs;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .httpClientBuilder(ApacheHttpClient.builder()
                        // Pool de conexões: suporta multipart upload paralelo (4 threads)
                        .maxConnections(20)
                        // Timeout para estabelecer conexão TCP com S3
                        .connectionTimeout(Duration.ofSeconds(5))
                        // Timeout de socket (tempo máximo entre pacotes)
                        // Generoso para uploads grandes em redes lentas
                        .socketTimeout(Duration.ofMinutes(5))
                        // Reutilizar conexões com keep-alive
                        .connectionMaxIdleTime(Duration.ofSeconds(60))
                        // Espera máxima por conexão do pool
                        .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                        // TCP keep-alive detecta conexões mortas
                        .tcpKeepAlive(true))
                .build();
    }

    /**
     * S3Presigner para gerar URLs pré-assinadas (PUT/GET). Usado pelo fluxo de
     * upload direto Laravel→S3 para PSB.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
    }
}
