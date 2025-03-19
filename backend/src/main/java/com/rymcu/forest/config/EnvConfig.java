package com.rymcu.forest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:.env")  // 如果文件在 resources 目录
// 或 @PropertySource("file:./.env")  // 如果文件在项目根目录
public class EnvConfig {}
