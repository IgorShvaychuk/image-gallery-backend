package com.tuki.imageapp.controller;

import com.tuki.imageapp.model.ImageMetadata;
import com.tuki.imageapp.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for image upload and search.
 */
@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Endpoint for uploading an image.
     * @param file The image file to upload.
     * @return A response entity with the uploaded image's metadata.
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageMetadata> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            ImageMetadata metadata = imageService.uploadImage(file);
            return ResponseEntity.ok(metadata);
        } catch (IOException e) {
            System.err.println("Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during upload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint for searching images by a label.
     * @param label The label to search for.
     * @return A list of image metadata matching the label.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ImageMetadata>> searchImages(@RequestParam String label) {
        List<ImageMetadata> results = imageService.searchImages(label);
        return ResponseEntity.ok(results);
    }

    /**
     * Endpoint for retrieving all unique labels from DynamoDB.
     * @return A list of unique labels.
     */
    @GetMapping("/labels")
    public ResponseEntity<List<String>> getAllLabels() {
        List<String> labels = imageService.getAllUniqueLabels();
        return ResponseEntity.ok(labels);
    }
}
