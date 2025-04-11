package com.sdumagicode.backend.service;

import com.sdumagicode.backend.dto.CodeTemplateDTO;

import java.util.List;

/**
 * 代码模板服务接口
 * 为在线IDE提供不同编程语言的代码模板
 */
public interface CodeTemplateService {
    
    /**
     * 获取所有支持的编程语言及其代码模板
     * @return 编程语言和代码模板列表
     */
    List<CodeTemplateDTO> getAllTemplates();
    
    /**
     * 根据语言获取代码模板
     * @param language 编程语言标识符（如java, cpp, python等）
     * @return 对应语言的代码模板，如果不支持该语言则返回null
     */
    CodeTemplateDTO getTemplateByLanguage(String language);
}