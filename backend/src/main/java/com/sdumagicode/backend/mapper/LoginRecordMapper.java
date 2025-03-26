package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.entity.LoginRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created on 2022/1/14 8:46.
 *
 * @author ronger
 * @email ronger-x@outlook.com
 * @packageName com.rymcu.forest.mapper
 */
public interface LoginRecordMapper extends Mapper<LoginRecord> {
    /**
     * 获取用户登录记录
     *
     * @param idUser
     * @return
     */
    List<LoginRecord> selectLoginRecordByIdUser(@Param("idUser") Integer idUser);
}
