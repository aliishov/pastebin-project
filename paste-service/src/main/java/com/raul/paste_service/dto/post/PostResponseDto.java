package com.raul.paste_service.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raul.paste_service.dto.tag.TagResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDto {
        private String title;
        private String slug;
        private String content;
        private String summary;
        private List<TagResponseDto> tags;
        private Integer userId;
        private Integer rating;
        private Integer likesCount;
        private Integer viewsCount;

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
        private LocalDateTime expirationDate;
        private String hash;
}


