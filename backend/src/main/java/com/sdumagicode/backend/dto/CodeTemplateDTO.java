package com.sdumagicode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeTemplateDTO {
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 代码模板内容
     */
    private String template;
    
    /**
     * 语言显示名称
     */
    private String displayName;
    
    /**
     * 语言图标（前端使用）
     */
    private String icon;
}