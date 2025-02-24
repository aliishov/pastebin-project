package com.raul.search_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PostResponseDto {
        private String title;
        private String slug;
        private String content;
        private String summary;
        private List<String> tags;
        private Integer userId;
        private Integer rating;
        private Integer likesCount;
        private Integer viewsCount;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime expirationDate;
        private String hash;
}


