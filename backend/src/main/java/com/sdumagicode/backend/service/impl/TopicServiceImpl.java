package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.exception.BusinessException;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.admin.TagDTO;
import com.sdumagicode.backend.dto.admin.TopicDTO;
import com.sdumagicode.backend.dto.admin.TopicTagDTO;
import com.sdumagicode.backend.entity.Tag;
import com.sdumagicode.backend.entity.Topic;
import com.sdumagicode.backend.enumerate.FileDataType;
import com.sdumagicode.backend.enumerate.FilePath;
import com.sdumagicode.backend.mapper.TopicMapper;
import com.sdumagicode.backend.service.TopicService;
import com.sdumagicode.backend.util.XssUtils;
import com.sdumagicode.backend.controller.common.UploadController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Condition;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author ronger
 */
@Service
public class TopicServiceImpl extends AbstractService<Topic> implements TopicService {

    @Resource
    private TopicMapper topicMapper;

    @Override
    public List<Topic> findTopicNav() {
        return topicMapper.selectTopicNav();
    }

    @Override
    public Topic findTopicByTopicUri(String topicUri) {
        Topic searchTopic = new Topic();
        searchTopic.setTopicUri(topicUri);
        return topicMapper.selectOne(searchTopic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Topic saveTopic(Topic topic) throws ServiceException {
        Integer result;
        topic.setTopicDescriptionHtml(XssUtils.filterHtmlCode(topic.getTopicDescriptionHtml()));
        if (topic.getIdTopic() == null) {
            if (StringUtils.isBlank(topic.getTopicTitle())) {
                throw new IllegalArgumentException("标签名不能为空!");
            } else {
                Condition topicCondition = new Condition(Topic.class);
                topicCondition.createCriteria().andCondition("topic_title =", topic.getTopicTitle());
                List<Topic> topics = topicMapper.selectByCondition(topicCondition);
                if (!topics.isEmpty()) {
                    throw new BusinessException("专题 '" + topic.getTopicTitle() + "' 已存在!");
                }
            }
            if (FileDataType.BASE64.equals(topic.getTopicImageType())) {
                String topicIconPath = UploadController.uploadBase64File(topic.getTopicIconPath(), FilePath.TOPIC);
                topic.setTopicIconPath(topicIconPath);
            } else {
                topic.setTopicIconPath(topic.getTopicIconPath());
            }
            topic.setTopicNva(topic.getTopicNva());
            topic.setTopicStatus(topic.getTopicStatus());
            topic.setTopicSort(topic.getTopicSort());
            topic.setTopicDescription(topic.getTopicDescription());
            topic.setTopicDescriptionHtml(topic.getTopicDescriptionHtml());
            topic.setCreatedTime(new Date());
            topic.setUpdatedTime(topic.getCreatedTime());
            result = topicMapper.insertSelective(topic);
        } else {
            if (FileDataType.BASE64.equals(topic.getTopicImageType())) {
                String topicIconPath = UploadController.uploadBase64File(topic.getTopicIconPath(), FilePath.TOPIC);
                topic.setTopicIconPath(topicIconPath);
            }
            topic.setUpdatedTime(new Date());
            result = topicMapper.update(topic.getIdTopic(), topic.getTopicTitle(), topic.getTopicUri()
                    , topic.getTopicIconPath(), topic.getTopicNva(), topic.getTopicStatus()
                    , topic.getTopicSort(), topic.getTopicDescription(), topic.getTopicDescriptionHtml());
        }
        if (result == 0) {
            throw new ServiceException("操作失败!");
        }
        return topic;
    }

    @Override
    public List<Tag> findUnbindTagsById(Long idTopic, String tagTitle) {
        if (StringUtils.isBlank(tagTitle)) {
            tagTitle = "";
        }
        return topicMapper.selectUnbindTagsById(idTopic, tagTitle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicTagDTO bindTopicTag(TopicTagDTO topicTag) throws ServiceException {
        Integer result = topicMapper.insertTopicTag(topicTag.getIdTopic(), topicTag.getIdTag());
        if (result == 0) {
            throw new ServiceException("操作失败!");
        }
        return topicTag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicTagDTO unbindTopicTag(TopicTagDTO topicTag) throws ServiceException {
        Integer result = topicMapper.deleteTopicTag(topicTag.getIdTopic(), topicTag.getIdTag());
        if (result == 0) {
            throw new ServiceException("操作失败!");
        }
        return topicTag;
    }

    @Override
    public List<TagDTO> findTagsByTopicUri(String topicUri) {
        TopicDTO topic = topicMapper.selectTopicByTopicUri(topicUri);
        if (topic == null) {
            return null;
        }
        return topicMapper.selectTopicTag(topic.getIdTopic());
    }
}
