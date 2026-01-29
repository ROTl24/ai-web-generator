package com.uloaix.xiaolu_aicode.core.parser;

import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 * 根据代码生成类型执行相应的解析逻辑
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();
    /**
     * 根据代码生成类型执行相应的解析逻辑
     * @param codeContent 代码内容
     * @param codeGenType 代码类型
     * @return 解析结果（HtmlCodeResult 或者 MultiFileCodeResult）
     */
    public static Object execute(String codeContent, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码类型");
        };
    }
}
