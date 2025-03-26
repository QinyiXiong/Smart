package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.dto.UserDTO;
import com.sdumagicode.backend.entity.Follow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ronger
 */
public interface FollowMapper extends Mapper<Follow> {
    /**
     * 判断是否关注
     *
     * @param followingId
     * @param followerId
     * @param followingType
     * @return
     */
    Boolean isFollow(@Param("followingId") Integer followingId, @Param("followerId") Long followerId, @Param("followingType") String followingType);

    /**
     * 查询用户粉丝
     *
     * @param idUser
     * @return
     */
    List<UserDTO> selectUserFollowersByUser(@Param("idUser") Long idUser);

    /**
     * 查询用户关注用户
     *
     * @param idUser
     * @return
     */
    List<UserDTO> selectUserFollowingsByUser(@Param("idUser") Long idUser);
}
