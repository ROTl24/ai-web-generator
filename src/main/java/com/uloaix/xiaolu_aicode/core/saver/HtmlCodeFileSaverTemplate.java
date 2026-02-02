package com.uloaix.xiaolu_aicode.core.saver;

import com.uloaix.xiaolu_aicode.ai.model.HtmlCodeResult;
import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;

import cn.hutool.core.util.StrUtil;

/**
 * HTML代码文件保存器
 *
 * @author xiaolu
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }
    /**
     * 保存 HTML 文件
     * @param result 代码结果对象
     * @param baseDirPath 保存目录
     * @throws BusinessException 如果保存失败
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    /**
     * 验证输入参数
     * @param result 代码结果对象
     * @throws BusinessException 如果输入参数不合法
     */
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}

