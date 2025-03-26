package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.entity.Role;
import com.sdumagicode.backend.entity.User;

import java.util.List;


/**
 * @author CodeGenerator
 * @date 2018/05/29
 */
public interface RoleService extends Service<Role> {

    /**
     * 查询用户角色
     *
     * @param user
     * @return
     */
    List<Role> selectRoleByUser(User user);

    /**
     * 查询用户角色
     *
     * @param idUser
     * @return
     */
    List<Role> findByIdUser(Long idUser);

    /**
     * 更新用户状态
     *
     * @param idRole
     * @param status
     * @return
     * @throws ServiceException
     */
    boolean updateStatus(Long idRole, String status) throws ServiceException;

    /**
     * 添加/更新角色
     *
     * @param role
     * @return
     * @throws ServiceException
     */
    boolean saveRole(Role role) throws ServiceException;
}
