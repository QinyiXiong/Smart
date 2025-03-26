package com.sdumagicode.backend.controller.user;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.security.annotation.SecurityInterceptor;
import com.sdumagicode.backend.dto.ChangeEmailDTO;
import com.sdumagicode.backend.dto.UpdatePasswordDTO;
import com.sdumagicode.backend.dto.UserInfoDTO;
import com.sdumagicode.backend.entity.LoginRecord;
import com.sdumagicode.backend.entity.UserExtend;
import com.sdumagicode.backend.service.LoginRecordService;
import com.sdumagicode.backend.service.UserService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ronger
 */
@RestController
@RequestMapping("/api/v1/user-info")
@RequiresPermissions(value = "user")
public class UserInfoController {

    @Resource
    private UserService userService;
    @Resource
    private LoginRecordService loginRecordService;

    @GetMapping("/detail/{idUser}")
    @SecurityInterceptor
    public GlobalResult<UserInfoDTO> detail(@PathVariable Long idUser) {
        UserInfoDTO userInfo = userService.findUserInfo(idUser);
        return GlobalResultGenerator.genSuccessResult(userInfo);
    }

    @GetMapping("/detail/{idUser}/extend-info")
    @SecurityInterceptor
    public GlobalResult<UserExtend> extendInfo(@PathVariable Long idUser) {
        UserExtend userExtend = userService.findUserExtendInfo(idUser);
        return GlobalResultGenerator.genSuccessResult(userExtend);
    }

    @GetMapping("/check-nickname")
    public GlobalResult<Boolean> checkNickname(@RequestParam Long idUser, @RequestParam String nickname) {
        boolean flag = userService.checkNicknameByIdUser(idUser, nickname);
        return GlobalResultGenerator.genSuccessResult(flag);
    }

    @PatchMapping("/update")
    @SecurityInterceptor
    public GlobalResult<UserInfoDTO> updateUserInfo(@RequestBody UserInfoDTO user) throws ServiceException {
        user = userService.updateUserInfo(user);
        return GlobalResultGenerator.genSuccessResult(user);
    }

    @PatchMapping("/update-extend")
    @SecurityInterceptor
    public GlobalResult<UserExtend> updateUserExtend(@RequestBody UserExtend userExtend) throws ServiceException {
        userExtend = userService.updateUserExtend(userExtend);
        return GlobalResultGenerator.genSuccessResult(userExtend);
    }

    @PatchMapping("/update-email")
    @SecurityInterceptor
    public GlobalResult<Boolean> updateEmail(@RequestBody ChangeEmailDTO changeEmailDTO) throws ServiceException {
        boolean flag = userService.updateEmail(changeEmailDTO);
        return GlobalResultGenerator.genSuccessResult(flag);
    }

    @PatchMapping("/update-password")
    @SecurityInterceptor
    public GlobalResult<Boolean> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO) {
        boolean flag = userService.updatePassword(updatePasswordDTO);
        return GlobalResultGenerator.genSuccessResult(flag);
    }

    @GetMapping("/login-records")
    @SecurityInterceptor
    public GlobalResult<PageInfo<LoginRecord>> loginRecords(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer rows, @RequestParam Integer idUser) {
        PageHelper.startPage(page, rows);
        List<LoginRecord> list = loginRecordService.findLoginRecordByIdUser(idUser);
        PageInfo<LoginRecord> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

}
