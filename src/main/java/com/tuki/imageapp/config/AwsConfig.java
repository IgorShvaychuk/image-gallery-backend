package com.tuki.imageapp.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Configuration for S3, Rekognition, and DynamoDB clients.
 * Uses DefaultAWSCredentialsProviderChain to automatically pick up credentials
 * from IAM Role (on EC2), environment variables, or ~/.aws/credentials.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentialsProvider())
                .withRegion(Regions.fromName(awsRegion))
                .build();
    }

    @Bean
    public AmazonRekognition amazonRekognition() {
        return AmazonRekognitionClientBuilder.standard()
                .withCredentials(awsCredentialsProvider())
                .withRegion(Regions.fromName(awsRegion))
                .build();
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withCredentials(awsCredentialsProvider())
                .withRegion(Regions.fromName(awsRegion))
                .build();
    }
}
