package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "聊天中的图片消息请求")
public class ImageChatMessageRequest {

    @NotBlank(message = "提示词不能为空")
    @Schema(description = "提示词", example = "宇航员在火星上喝奶茶，像素风")
    private String prompt;

    @NotEmpty(message = "图片URL不能为空")
    @Schema(description = "图片URL列表")
    private List<String> imageUrls;
}
