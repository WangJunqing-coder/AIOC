package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.dto.request.VideoGenerationRequest;
import com.kmu.edu.back_service.dto.response.VideoGenerationResponse;
import com.kmu.edu.back_service.entity.VideoGeneration;

import java.util.List;

public interface VideoGenerationService {
    VideoGenerationResponse generateVideo(VideoGenerationRequest request);
    VideoGenerationResponse getGenerationStatus(Long id);
    List<VideoGeneration> getUserGenerations(Long userId, Integer page, Integer size);
    void deleteGeneration(Long id);
    void processGenerationTask(Long id);
}
