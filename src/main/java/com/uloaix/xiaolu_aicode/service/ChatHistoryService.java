package com.uloaix.xiaolu_aicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.uloaix.xiaolu_aicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.uloaix.xiaolu_aicode.model.entity.ChatHistory;
import com.uloaix.xiaolu_aicode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/ROTl24">程序员小陆</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     *  添加聊天记录
     * @param appId 应用Id
     * @param message 消息
     * @param messageType 消息类别
     * @param userId 用户Id
     * @return 是否成功
     */
    boolean addChatMessage(Long appId,String message,String messageType,Long userId);


    /**
     * 根据应用Id 删除应用聊天记录
     * @param appId 应用Id
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 分页获取应用聊天记录
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser) ;

    /**
     * 加载聊天记录到内存
     * @param appId 应用Id
     * @param chatMemory 历史聊天
     * @param maxCount 最大数量
     * @return
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
