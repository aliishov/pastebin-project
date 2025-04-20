package com.raul.search_service.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "posts")
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String slug;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String content;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String summary;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Text)
    private String hash;

    @Field(type = FieldType.Integer)
    private Integer userId;

    @Field(type = FieldType.Integer)
    private Integer rating;

    @Field(type = FieldType.Integer)
    private Integer likesCount;

    @Field(type = FieldType.Integer)
    private Integer viewsCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime expiresAt;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;
}
