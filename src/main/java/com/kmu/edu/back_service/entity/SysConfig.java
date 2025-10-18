package com.kmu.edu.back_service.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
public class SysConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String configDesc;
    private Integer status; // 0禁用 1启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
