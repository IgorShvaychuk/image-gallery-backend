package com.tuki.imageapp.service;

import org.springframework.stereotype.Service;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tuki.imageapp.model.ImageMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling image upload, Rekognition analysis, and DynamoDB operations.
 */
@Service
public class ImageService {

    private final AmazonS3 s3Client;
    private final AmazonRekognition rekognitionClient;
    private final DynamoDBMapper dynamoDBMapper;

    @Value("${aws.s3.bucketName}")
    private String s3BucketName;

    public ImageService(AmazonS3 s3Client, AmazonRekognition rekognitionClient, AmazonDynamoDB dynamoDBClient) {
        this.s3Client = s3Client;
        this.rekognitionClient = rekognitionClient;
        this.dynamoDBMapper = new DynamoDBMapper(dynamoDBClient);
    }

    /**
     * Uploads an image to S3, analyzes it with Rekognition, and stores metadata in DynamoDB.
     * @param file The image file to upload.
     * @return The metadata of the uploaded image.
     * @throws IOException If there's an error reading the file.
     */
    public ImageMetadata uploadImage(MultipartFile file) throws IOException {
        String imageId = UUID.randomUUID().toString();
        String s3Key = "uploads/" + imageId + "-" + file.getOriginalFilename();

        // Upload to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        s3Client.putObject(new PutObjectRequest(s3BucketName, s3Key, file.getInputStream(), metadata));

        String imageUrl = s3Client.getUrl(s3BucketName, s3Key).toString();
        System.out.println("Uploaded to S3: " + imageUrl);

        // Analyze with Rekognition
        List<String> labels = analyzeImageWithRekognition(s3BucketName, s3Key);
        System.out.println("Rekognition Labels: " + labels);

        // Store metadata in DynamoDB
        ImageMetadata imageMetadata = new ImageMetadata();
        imageMetadata.setImageId(imageId);
        imageMetadata.setImageUrl(imageUrl);
        imageMetadata.setOriginalFileName(file.getOriginalFilename());
        imageMetadata.setLabels(labels);

        dynamoDBMapper.save(imageMetadata);
        System.out.println("Metadata saved to DynamoDB for imageId: " + imageId);

        return imageMetadata;
    }

    /**
     * Helper method to analyze an image using Amazon Rekognition's DetectLabels.
     * @param bucket The S3 bucket where the image is stored.
     * @param key The key (path) of the image in the S3 bucket.
     * @return A list of detected labels (objects).
     */
    private List<String> analyzeImageWithRekognition(String bucket, String key) {
        try {
            Image image = new Image()
                    .withS3Object(new S3Object()
                            .withBucket(bucket)
                            .withName(key));

            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(image)
                    .withMaxLabels(10) // Max 10 labels to detect
                    .withMinConfidence(70F); // Minimum confidence score for a label to be returned (70%)

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            return result.getLabels().stream()
                    .map(Label::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error analyzing image with Rekognition: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches for images in DynamoDB based on provided labels.
     * @param searchLabel The label to search for.
     * @return A list of ImageMetadata matching the search criteria.
     */
    public List<ImageMetadata> searchImages(String searchLabel) {
        if (searchLabel == null || searchLabel.trim().isEmpty()) {
            return Collections.emptyList();
        }

        HashMap<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        // Convert search label to lowercase for case-insensitive comparison if labels are stored as lowercase
        expressionAttributeValues.put(":searchLabel", new AttributeValue().withS(searchLabel.toLowerCase()));

        // DynamoDB scan expression
        // 'contains(labels, :searchLabel)' searches for items where the 'labels' list contains ': searchLabel'
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("contains(labels, :searchLabel)")
                .withExpressionAttributeValues(expressionAttributeValues);

        return dynamoDBMapper.scan(ImageMetadata.class, scanExpression);
    }

    /**
     * Retrieves all unique labels from all images in DynamoDB.
     * @return A list of unique labels.
     */
    public List<String> getAllUniqueLabels() {
        // Perform a scan operation to get all items
        List<ImageMetadata> allImages = dynamoDBMapper.scan(ImageMetadata.class, new DynamoDBScanExpression());

        // Collect all labels into a Set to ensure uniqueness
        Set<String> uniqueLabels = new HashSet<>();
        for (ImageMetadata image : allImages) {
            if (image.getLabels() != null) {
                uniqueLabels.addAll(image.getLabels());
            }
        }
        // Convert the Set of unique labels to a List and return
        return new java.util.ArrayList<>(uniqueLabels);
    }
}
