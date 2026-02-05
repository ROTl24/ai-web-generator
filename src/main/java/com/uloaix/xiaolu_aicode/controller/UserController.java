package com.uloaix.xiaolu_aicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.uloaix.xiaolu_aicode.annotation.AuthCheck;
import com.uloaix.xiaolu_aicode.common.BaseResponse;
import com.uloaix.xiaolu_aicode.common.DeleteRequest;
import com.uloaix.xiaolu_aicode.common.ResultUtils;
import com.uloaix.xiaolu_aicode.constant.UserConstant;
import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.exception.ThrowUtils;
import com.uloaix.xiaolu_aicode.manager.CosManager;
import com.uloaix.xiaolu_aicode.model.dto.user.*;
import com.uloaix.xiaolu_aicode.model.entity.User;
import com.uloaix.xiaolu_aicode.model.vo.LoginUserVO;
import com.uloaix.xiaolu_aicode.model.vo.UserVO;
import com.uloaix.xiaolu_aicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/ROTl24?tab=repositories">小陆</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 用户注册
     * 该方法处理用户注册请求，验证参数并调用服务层完成注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 上传用户头像
     *
     * @param file    头像文件
     * @param request 请求
     * @return 头像访问 URL
     */
    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadUserAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        userService.getLoginUser(request);
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "头像文件不能为空");
        long maxSize = 2 * 1024 * 1024L;
        ThrowUtils.throwIf(file.getSize() > maxSize, ErrorCode.PARAMS_ERROR, "头像文件不能超过 2MB");
        String contentType = file.getContentType();
        boolean isJpg = "image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType);
        boolean isPng = "image/png".equalsIgnoreCase(contentType);
        ThrowUtils.throwIf(!(isJpg || isPng), ErrorCode.PARAMS_ERROR, "仅支持 JPG/PNG 格式的图片");
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        if (StrUtil.isBlank(suffix)) {
            suffix = isPng ? "png" : "jpg";
        }
        File tempFile = null;
        try {
            tempFile = File.createTempFile("avatar_", "." + suffix);
            file.transferTo(tempFile);
            String cosKey = generateAvatarKey(suffix);
            String avatarUrl = cosManager.uploadFile(cosKey, tempFile);
            ThrowUtils.throwIf(StrUtil.isBlank(avatarUrl), ErrorCode.OPERATION_ERROR, "头像上传失败");
            return ResultUtils.success(avatarUrl);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "头像上传失败");
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }

    /**
     * 添加用户（仅管理员）
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新当前登录用户（普通用户）
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateMyRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(userUpdateMyRequest.getUserName()),
                ErrorCode.PARAMS_ERROR, "用户名不能为空");
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserName(userUpdateMyRequest.getUserName());
        user.setUserAvatar(userUpdateMyRequest.getUserAvatar());
        user.setUserProfile(userUpdateMyRequest.getUserProfile());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 生成头像的对象存储键
     * 格式：/avatars/2025/07/31/uuid.jpg
     */
    private String generateAvatarKey(String suffix) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "." + suffix;
        return String.format("/avatars/%s/%s", datePath, fileName);
    }


//    /**
//     * 保存用户。
//     *
//     * @param user 用户
//     * @return {@code true} 保存成功，{@code false} 保存失败
//     */
//    @PostMapping("save")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public boolean save(@RequestBody User user) {
//        return userService.save(user);
//    }

//    /**
//     * 根据主键删除用户。
//     *
//     * @param id 主键
//     * @return {@code true} 删除成功，{@code false} 删除失败
//     */
//    @DeleteMapping("remove/{id}")
//    public boolean remove(@PathVariable Long id) {
//        return userService.removeById(id);
//    }

//    /**
//     * 根据主键更新用户。
//     *
//     * @param user 用户
//     * @return {@code true} 更新成功，{@code false} 更新失败
//     */
//    @PutMapping("update")
//    public boolean update(@RequestBody User user) {
//        return userService.updateById(user);
//    }
//
//    /**
//     * 查询所有用户。
//     *
//     * @return 所有数据
//     */
//    @GetMapping("list")
//    public List<User> list() {
//        return userService.list();
//    }

//    /**
//     * 根据主键获取用户。
//     *
//     * @param id 用户主键
//     * @return 用户详情
//     */
//    @GetMapping("getInfo/{id}")
//    public User getInfo(@PathVariable Long id) {
//        return userService.getById(id);
//    }






}
