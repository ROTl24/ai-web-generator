package com.uloaix.xiaolu_aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.mapper.AppMapper;
import com.uloaix.xiaolu_aicode.model.dto.app.AppQueryRequest;
import com.uloaix.xiaolu_aicode.model.entity.App;
import com.uloaix.xiaolu_aicode.model.entity.User;
import com.uloaix.xiaolu_aicode.model.vo.AppVO;
import com.uloaix.xiaolu_aicode.model.vo.UserVO;
import com.uloaix.xiaolu_aicode.service.AppService;
import com.uloaix.xiaolu_aicode.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/ROTl24">程序员小陆</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Override
    public void validApp(App app, boolean add) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用信息为空");
        }
        String initPrompt = app.getInitPrompt();
        if (add && StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取脱敏后的应用信息
     * 
     * @param app 应用
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        // 复制应用信息
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        // 如果用户id不为空，则查询用户信息
        if (userId != null) {
            User user = userService.getById(userId);
            // 复制用户信息
            UserVO userVO = userService.getUserVO(user);
            // 设置用户信息
            appVO.setUser(userVO);
        }
        // 返回脱敏后的应用信息
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                // 获取用户id  
                .map(App::getUserId)
                // 将用户id转换为set
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                // 将用户id和用户信息映射到map中
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 将应用列表转换为应用信息列表
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

}
