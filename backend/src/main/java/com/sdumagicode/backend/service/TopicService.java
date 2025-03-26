package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.dto.admin.TagDTO;
import com.sdumagicode.backend.dto.admin.TopicTagDTO;
import com.sdumagicode.backend.entity.Tag;
import com.sdumagicode.backend.entity.Topic;

import java.util.List;

/**
 * @author ronger
 */
public interface TopicService extends Service<Topic> {

    /**
     * 获取导航主题数据
     *
     * @return
     */
    List<Topic> findTopicNav();

    /**
     * 根据 topicUri 获取主题信息及旗下标签数据
     *
     * @param topicUri 主题 URI
     * @return
     */
    Topic findTopicByTopicUri(String topicUri);

    /**
     * 新增/更新主题信息
     *
     * @param topic 主题信息
     * @return
     * @throws ServiceException
     */
    Topic saveTopic(Topic topic) throws ServiceException;

    /**
     * 查询未绑定标签
     *
     * @param idTopic
     * @param tagTitle
     * @return
     */
    List<Tag> findUnbindTagsById(Long idTopic, String tagTitle);

    /**
     * 绑定标签
     *
     * @param topicTag
     * @return
     * @throws ServiceException
     */
    TopicTagDTO bindTopicTag(TopicTagDTO topicTag) throws ServiceException;

    /**
     * 取消绑定标签
     *
     * @param topicTag
     * @return
     * @throws ServiceException
     */
    TopicTagDTO unbindTopicTag(TopicTagDTO topicTag) throws ServiceException;

    /**
     * 获取主题下标签列表
     *
     * @param topicUri
     * @return
     */
    List<TagDTO> findTagsByTopicUri(String topicUri);
}
