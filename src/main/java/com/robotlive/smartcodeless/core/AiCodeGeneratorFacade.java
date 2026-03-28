package com.robotlive.smartcodeless.core;

import com.robotlive.smartcodeless.ai.AiCodeGeneratorService;
import com.robotlive.smartcodeless.ai.AiCodeGeneratorServiceFactory;
import com.robotlive.smartcodeless.ai.model.HtmlCodeResult;
import com.robotlive.smartcodeless.ai.model.MultiFileCodeResult;
import com.robotlive.smartcodeless.core.parser.CodeParserExecutor;
import com.robotlive.smartcodeless.core.saver.CodeFileSaverExecutor;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;



@Service
@Slf4j
public class AiCodeGeneratorFacade {

    /**
     * 门面模式(外观模式), 客户端不关心内部实现逻辑，只需要调用Facade即可
     */
//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "传入参数为空");
        }

        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage); // AI生成的方法体
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId); // 保存文件
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码， 流式调用
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"传入参数为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                // 执行AI流式转换和文件保存
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                // 执行AI流式转换和文件保存
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 继续封装, 根据codeGenTypeEnum
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        // result
        // 因为是SSE流式输出, 需要自己拼接String然后返回Flux, 这个result不是方法结构体
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk ->{
            codeBuilder.append(chunk);
        }).doOnComplete(() ->{
            try {
                String completeHtmlCode = codeBuilder.toString();
                // 执行执行器
                Object object = CodeParserExecutor.executeParser(completeHtmlCode, codeGenTypeEnum);
                // 执行文件保存器
                File saveDir = CodeFileSaverExecutor.executeSaver(object, codeGenTypeEnum, appId);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            }catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }


//    /**
//     * 门面设计模式
//     */
//
//    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        // result
//        // 因为是SSE流式输出, 需要自己拼接String然后返回Flux, 这个result不是方法结构体
//        StringBuilder codeBuilder = new StringBuilder();
//        return result.doOnNext(chunk -> {
//            codeBuilder.append(chunk);
//        }).doOnComplete(() -> {
//                    // 流式返回完成后，保存代码
//                    try {
//                        String completeHtmlCode = codeBuilder.toString();
//                        // 解析代码为对象
//                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
//                        // 保存代码到文件
//                        File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//                        log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存失败: {}", e.getMessage());
//                    }
//                });
//    }
//
//    /**
//     * 生成多文件模式的代码并保存（流式）
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return result.doOnNext(chunk -> {
//            // 实时收集代码片段
//            codeBuilder.append(chunk);
//        }).doOnComplete(() -> {
//            try {
//                // 流式返回完成后，保存代码
//                String completeMultiFileCode = codeBuilder.toString();
//                // 解析代码为对象
//                MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
//                // 保存代码到文件
//                File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
//                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//            } catch (Exception e) {
//                log.error("保存失败: {}", e.getMessage());
//            }
//        });
//    }

//    /**
//     * 生成 HTML 模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(result);
//    }
//
//    /**
//     * 生成多文件模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiFileCodeResult(result);
//    }

}