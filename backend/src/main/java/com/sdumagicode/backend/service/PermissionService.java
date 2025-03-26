package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.entity.Permission;
import com.sdumagicode.backend.entity.User;

import java.util.List;


/**
 * @author CodeGenerator
 * @date 2018/05/29
 */
public interface PermissionService extends Service<Permission> {

    /**
     * 获取用户权限
     *
     * @param sysUser
     * @return
     */
    List<Permission> selectPermissionByUser(User sysUser);
}
