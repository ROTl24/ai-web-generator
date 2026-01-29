package com.uloaix.xiaolu_aicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.uloaix.xiaolu_aicode.model.dto.AppQueryRequest;
import com.uloaix.xiaolu_aicode.model.entity.App;
import com.uloaix.xiaolu_aicode.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/ROTl24">程序员小陆</a>
 */
public interface AppService extends IService<App> {

    /**
     * 校验应用
     *
     * @param app 应用信息
     * @param add 是否为创建
     */
    void validApp(App app, boolean add);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取脱敏后的应用信息
     *
     * @param app 应用
     * @return 脱敏后的应用信息
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息列表
     *
     * @param appList 应用列表
     * @return 脱敏后的应用信息列表
     */
    List<AppVO> getAppVOList(List<App> appList);
}
