package com.sdumagicode.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import org.apache.ibatis.annotations.*;

import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface ChatMapper extends BaseMapper<ChatRecords> {

        @Select("SELECT chat_id as chatId, user_id as userId, interviewer_id as interviewerId, created_at as createdAt, updated_at as updatedAt, topic "
                        +
                        "FROM interview_chat_records " +
                        "WHERE user_id = #{userId} AND interviewer_id = #{chatRecords.interviewerId}")
        List<ChatRecords> selectChatRecords(@Param("userId") Long userId,
                        @Param("chatRecords") ChatRecords chatRecords);

        @Select("SELECT chat_id as chatId, user_id as userId, interviewer_id as interviewerId, created_at as createdAt, updated_at as updatedAt, topic" +
                " FROM interview_chat_records WHERE chat_id = #{id}")
        ChatRecords selectById(@Param("id") Long id);

        @Insert("INSERT INTO interview_chat_records (user_id, interviewer_id, created_at, updated_at, topic) " +
                        "VALUES (#{userId}, #{interviewerId}, #{createdAt}, #{updatedAt}, #{topic})")
        @Options(useGeneratedKeys = true, keyProperty = "chatId", keyColumn = "chat_id")
        int insertChatRecord(ChatRecords chatRecords);

        @Update("UPDATE interview_chat_records SET " +
                        "user_id = #{userId}, " +
                        "interviewer_id = #{interviewerId}, " +
                        "updated_at = #{updatedAt}, " +
                        "topic = #{topic} " +
                        "WHERE chat_id = #{chatId}")
        int updateChatRecord(ChatRecords chatRecords);

        @Delete("DELETE FROM interview_chat_records WHERE chat_id = #{chatId}")
        int deleteChatRecord(@Param("chatId") Long chatId);

}
