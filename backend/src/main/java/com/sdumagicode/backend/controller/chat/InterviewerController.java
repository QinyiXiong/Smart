package com.sdumagicode.backend.controller.chat;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/Interviewer")
@RequiresPermissions(value = "user")
public class InterviewerController {
}
