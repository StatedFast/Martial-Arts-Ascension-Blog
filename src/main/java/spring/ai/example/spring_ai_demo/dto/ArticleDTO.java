package spring.ai.example.spring_ai_demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleDTO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String summary;
    private String coverImage;
    private String category;
    private String tags;
    private Integer status;
    private String authorName;
    private String authorAvatar;
}
