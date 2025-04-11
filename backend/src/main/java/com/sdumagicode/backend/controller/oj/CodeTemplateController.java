package com.sdumagicode.backend.controller.oj;

import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.CodeTemplateDTO;
import com.sdumagicode.backend.service.CodeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代码模板控制器
 * 为在线IDE提供不同编程语言的代码模板
 */
@RestController
@RequestMapping("/api/v1/code-templates")
public class CodeTemplateController {

    @Autowired
    private CodeTemplateService codeTemplateService;

    /**
     * 获取所有支持的编程语言及其代码模板
     * @return 编程语言和代码模板列表
     */
    @GetMapping
    public GlobalResult<List<CodeTemplateDTO>> getAllTemplates() {
        List<CodeTemplateDTO> templates = codeTemplateService.getAllTemplates();
        return GlobalResultGenerator.genSuccessResult(templates);
    }

    /**
     * 根据语言获取代码模板
     * @param language 编程语言标识符（如java, cpp, python等）
     * @return 对应语言的代码模板
     */
    @GetMapping("/{language}")
    public GlobalResult<CodeTemplateDTO> getTemplateByLanguage(@PathVariable String language) {
        CodeTemplateDTO template = codeTemplateService.getTemplateByLanguage(language);
        if (template == null) {
            return GlobalResultGenerator.genErrorResult("不支持的编程语言: " + language);
        }
        return GlobalResultGenerator.genSuccessResult(template);
    }
}