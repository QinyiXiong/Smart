package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.dto.CodeTemplateDTO;
import com.sdumagicode.backend.service.CodeTemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码模板服务实现类
 * 为在线IDE提供不同编程语言的代码模板
 */
@Service
public class CodeTemplateServiceImpl implements CodeTemplateService {

    // 存储各语言的代码模板
    private final Map<String, CodeTemplateDTO> templateMap = new HashMap<>();

    public CodeTemplateServiceImpl() {
        // 初始化各语言的代码模板
        initTemplates();
    }

    /**
     * 初始化各语言的代码模板
     */
    private void initTemplates() {
        // Java 代码模板
        CodeTemplateDTO javaTemplate = CodeTemplateDTO.builder()
                .language("java")
                .displayName("Java")
                .icon("el-icon-platform-eleme")
                .template(
                        "import java.util.*;\n" +
                        "import java.io.*;\n\n" +
                        "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        Scanner scanner = new Scanner(System.in);\n" +
                        "        \n" +
                        "        // 在这里编写你的代码\n" +
                        "        \n" +
                        "    }\n" +
                        "}\n")
                .build();

        // C++ 代码模板
        CodeTemplateDTO cppTemplate = CodeTemplateDTO.builder()
                .language("cpp")
                .displayName("C++")
                .icon("el-icon-cpu")
                .template(
                        "#include <iostream>\n" +
                        "#include <vector>\n" +
                        "#include <string>\n" +
                        "#include <algorithm>\n\n" +
                        "using namespace std;\n\n" +
                        "int main() {\n" +
                        "    // 在这里编写你的代码\n" +
                        "    \n" +
                        "    return 0;\n" +
                        "}\n")
                .build();

        // Python 代码模板
        CodeTemplateDTO pythonTemplate = CodeTemplateDTO.builder()
                .language("python")
                .displayName("Python")
                .icon("el-icon-magic-stick")
                .template(
                        "# 导入需要的库\n" +
                        "import sys\n" +
                        "\n" +
                        "# 在这里编写你的代码\n" +
                        "\n" +
                        "# 读取输入示例\n" +
                        "# line = input()\n" +
                        "# n = int(input())\n" +
                        "# nums = list(map(int, input().split()))\n")
                .build();

        // 将模板添加到映射中
        templateMap.put(javaTemplate.getLanguage(), javaTemplate);
        templateMap.put(cppTemplate.getLanguage(), cppTemplate);
        templateMap.put(pythonTemplate.getLanguage(), pythonTemplate);
    }

    @Override
    public List<CodeTemplateDTO> getAllTemplates() {
        return new ArrayList<>(templateMap.values());
    }

    @Override
    public CodeTemplateDTO getTemplateByLanguage(String language) {
        return templateMap.get(language);
    }
}