package com.achobeta.themis.domain.review.service.impl;

import com.achobeta.themis.common.Constant;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.util.AliOSSUtil;
import com.achobeta.themis.domain.review.model.entity.FileRecord;
import com.achobeta.themis.domain.review.model.entity.ResourceMultipartFile;
import com.achobeta.themis.domain.review.repo.IFileRepository;
import com.achobeta.themis.domain.review.service.IFileService;
import com.aliyuncs.exceptions.ClientException;
import com.itextpdf.kernel.pdf.PdfDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.achobeta.themis.common.Constant.*;
import static org.apache.commons.compress.utils.ArchiveUtils.sanitize;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final AliOSSUtil aliOSSUtil;
    private final IFileRepository fileRepository;

    /**
     * 保存文件到OSS
     * @param file 文件
     * @param conversationId 对话ID
     * @return OSS文件路径
     * @throws IOException IO异常
     */
    private String saveToOSS(ResourceMultipartFile file, String safeName,String conversationId) throws IOException {
        String ossPath = null;
        try {
            ossPath = aliOSSUtil.upload(conversationId, file, safeName);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        log.info("文件已保存到OSS：{}", ossPath);
        return ossPath;
    }

    /**
     * 检查本地是否存在文件
     * @param fileName 文件名
     * @param conversationId 对话ID
     * @return 是否存在
     */
    private boolean checkLocalFileExists(String fileName, String conversationId) {
        Path base = Paths.get(SYSTEM_LOCAL_PATH);
        Path path = base.resolve(sanitize(conversationId)).resolve(fileName);
        return Files.exists(path);
    }

    /**
     * 保存文件到本地
     * @param file 文件
     * @param conversationId 对话ID
     * @return 本地文件路径
     * @throws IOException IO异常
     */
    private String saveToLocal(ResourceMultipartFile file, String safeName, String conversationId) throws IOException {
        Path base = Paths.get("D:\\A\\ruku\\upload");
        Path dir = base.resolve(sanitize(conversationId));
        Files.createDirectories(dir);
        Path dest = dir.resolve(safeName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("文件已保存到本地：{}", dest.toAbsolutePath());
        return dest.toAbsolutePath().toString();
    }

    /**
     * 生成文件下载资源
     * @param isChange
     * @param conversationId
     * @param contentType
     * @param content
     * @return
     */
    @Override
    public Resource generateDownloadResource(Boolean isChange, String conversationId, String contentType, String content) {
        // 查询数据库是否存在文件记录
        FileRecord fileRecord = fileRepository.findByConversationIdAndContentType(conversationId, contentType);
        if (fileRecord != null && !isChange) {
            // 检查用户信息是否一致
            Long userId = null;
            try {
                userId = (Long) ((Map<String, Object>) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).get("id");
            } catch (Exception e) {
                log.error("从SecurityContextHolder获取用户ID失败：{}", e);
                throw new BusinessException("从SecurityContextHolder获取用户ID失败", e);
            }
            if (!userId.equals(fileRecord.getUserId())) {
                throw new BusinessException("您没有权限访问此文件");
            }

            String fileName = fileRecord.getFileName();
            Path dest = Paths.get(SYSTEM_LOCAL_PATH, conversationId + "." + contentType);
            if (!Files.exists(dest)) {
                // 本地不存在文件，从OSS下载到本地
                try {
                    byte[] file = aliOSSUtil.download(APP + "/" + conversationId + "." + contentType);
                    // 保存文件到本地
                    Files.createDirectories(dest.getParent());
                    Files.write(dest, file);
                } catch (ClientException | IOException e) {
                    log.error("从OSS下载文件到本地失败：{}", e);
                    throw new BusinessException("从OSS下载文件到本地失败", e);
                }

            }
            return new FileSystemResource(dest.toFile());
        } else {
            if (fileRecord != null){
                Path dest = Paths.get(SYSTEM_LOCAL_PATH, conversationId, fileRecord.getFileName());
                try {
                    Files.deleteIfExists(dest);
                } catch (IOException e) {
                    log.error("删除本地文件失败：{}", e);
                    throw new BusinessException("删除本地文件失败", e);
                }
            }
            // 根据 contentType 和 content 生成文件
            ResourceMultipartFile file = null;
            if (contentType.equals("pdf")) {
                file = pdfGenerator(conversationId, content);
            } else if (contentType.equals("docx")) {
                file = wordGenerator(conversationId, content);
            } else {
                throw new BusinessException("不支持的文件类型");
            }

            if(file == null) {
                throw new BusinessException("生成文件失败");
            }

            String ossPath = null;
            try {
                // 保存文件到OSS
                ossPath = saveToOSS(file, conversationId + "." + contentType, conversationId);
            } catch (IOException e) {
                throw new BusinessException("保存文件到OSS失败", e);
            }
            // 如果有数据更新文件路径，文件名
            if(fileRecord != null) {
                fileRecord.setFileName(conversationId + "." + contentType);
                fileRecord.setFileOssPath(ossPath);
                fileRecord.setCreateTime(LocalDateTime.now());
                fileRepository.updateById(fileRecord);
            } else {
                Long userId = (Long) ((Map<String, Object>) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).get("id");
                // 插入文件记录
                fileRepository.insert(FileRecord.builder()
                        .conversationId(conversationId)
                        .userId(userId)
                        .fileName(conversationId + "." + contentType)
                        .fileOssPath(ossPath)
                        .fileType(contentType)
                        .createTime(LocalDateTime.now())
                        .build());
            }
            log.info("文件已保存到OSS：{}", ossPath);

            Path dest = Paths.get(SYSTEM_LOCAL_PATH, conversationId + "." + contentType);
            return new FileSystemResource(dest.toFile());
        }
    }

    /**
     * 内容转换 (把“\\n”替换为“\n”)
     * @param content
     * @return
     */
    @Override
    public String contentTransform(String content) {
        return content.replaceAll("\\\\n", "\n");
    }

    @Deprecated
    private UrlResource generateUrlResource(String path) {
        try {
            return new UrlResource(path);
        } catch (MalformedURLException e) {
            throw new BusinessException("生成URL资源失败", e);
        }
    }

    private ResourceMultipartFile wordGenerator(String conversationId, String content) {
        String WORD_SAVE_PATH = SYSTEM_LOCAL_PATH + File.separator + conversationId + ".docx";
        System.out.println(WORD_SAVE_PATH);
        // 确保父目录存在
        if(!extracted(WORD_SAVE_PATH)) {
            throw new BusinessException("创建Word目录失败");
        }
        try (XWPFDocument doc = new XWPFDocument(); FileOutputStream out = new FileOutputStream(WORD_SAVE_PATH)) {
            String[] contentLines = content.split("\n");
            for (String line : contentLines) {
                XWPFParagraph p = doc.createParagraph();
                // 标题（居中）
                if (line.equals("劳动合同")) {
                    p.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun r = p.createRun();
                    r.setFontFamily("SimSun");
                    r.setFontSize(16);
                    r.setBold(true);
                    r.setText(line);
                    r.addCarriageReturn();
                }
                // 章节标题
                else if (line.matches("^[一二三四五六七八九十]+、.*")) {
                    p.setAlignment(ParagraphAlignment.LEFT);
                    XWPFRun r = p.createRun();
                    r.setFontFamily("SimSun");
                    r.setFontSize(12);
                    r.setBold(true);
                    r.setText(line);
                }
                // 普通正文/空行
                else {
                    p.setAlignment(ParagraphAlignment.LEFT);
                    XWPFRun r = p.createRun();
                    r.setFontFamily("SimSun");
                    r.setFontSize(10);
                    r.setText(line);
                }
            }

            doc.write(out);
            log.info("Word 生成成功！存储路径：{}", WORD_SAVE_PATH);
        } catch (IOException e) {
            log.error("Word 生成失败：文件写入异常！{}", e.getMessage());
        } catch (Exception e) {
            log.error("Word 生成失败：未知异常！{}", e.getMessage());
            e.printStackTrace();
        }
        return new ResourceMultipartFile(new FileSystemResource(WORD_SAVE_PATH), conversationId + ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", conversationId);
    }

    private ResourceMultipartFile pdfGenerator(String conversationId, String content) {
        String PDF_SAVE_PATH = SYSTEM_LOCAL_PATH + File.separator + conversationId + ".pdf";
        // 确保父目录存在
        if(!extracted(PDF_SAVE_PATH)) {
            throw new BusinessException("创建PDF目录失败");
        }
        // 使用 try-with-resources 确保资源正确关闭
        try (PdfWriter writer = new PdfWriter(PDF_SAVE_PATH);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            document.setMargins(20, 20, 20, 20); // 上、右、下、左

            // 优先尝试加载中文字体：classpath -> Windows 字体目录 -> 回退
            PdfFont chineseFont = null;
//            try {
//                byte[] fontBytes = null;
//                boolean fontCreatedDirectly = false;
//                if (fontBytes == null) {
//                    for (String sysPath : SYSTEM_FONTS) {
//                        Path p = Paths.get(sysPath);
//                        if (Files.exists(p)) {
//                            // 如果是 TTC 集合，直接使用路径并指定索引（例如 ,0）
//                            if (sysPath.toLowerCase().endsWith(".ttc")) {
//                                try {
//                                    chineseFont = PdfFontFactory.createFont(sysPath + ",0", PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
//                                    fontCreatedDirectly = true;
//                                    break;
//                                } catch (IOException ignored) {
//                                    // 若直接用路径失败，继续尝试读取字节或下一个候选
//                                    log.warn("字体目录加载字体 {} 失败", sysPath);
//                                }
//                            }
//                            try {
//                                fontBytes = Files.readAllBytes(p);
//                                break;
//                            } catch (IOException ignored) {
//                                // 继续尝试下一个
//                                log.warn("字体目录加载字体 {} 失败", sysPath);
//                            }
//                        }
//                    }
//                }
//
//                // 3) 若已通过路径创建字体（如 TTC），跳过字节创建；否则若找到字节数组，用字节数组创建字体；否则尝试使用原始 CHINESE_FONT_PATH（资源 id），若仍失败回退到内置字体
//                if (!fontCreatedDirectly) {
//                    if (fontBytes != null) {
//                        chineseFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
//                    } else {
//                        try {
//                            chineseFont = PdfFontFactory.createFont("com/itextpdf/fonts/asian/simsun.ttc,1", PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
//                        } catch (IOException ioe) {
//                            log.warn("警告：中文字体加载失败，使用内置字体替代。错误：{}", ioe.getMessage());
//                            chineseFont = PdfFontFactory.createFont();
//                        }
//                    }
//                }
//            } catch (IOException ioe) {
//                log.warn("警告：读取字体时出现异常，使用内置字体替代。错误：{}", ioe.getMessage());
//                try {
//                    chineseFont = PdfFontFactory.createFont();
//                } catch (IOException e2) {
//                    // 极端情况：连内置字体也无法创建，打印并抛出运行时异常
//                    log.error("致命：无法创建任何字体：{}", e2.getMessage());
//                    throw new RuntimeException(e2);
//                }
//            }

            // 保证 chineseFont 已初始化（防止编译器因不可确定赋值报错）
            // 使用classpath找到字体文件

            String resourceFontPath = "fronts" + File.separator + "simsun.ttc";
            //String fontPath = Optional.ofNullable(Constant.class.getClassLoader().getResource(resourceFontPath)).map(URL::getPath).orElse(null);
            String fontPath = Optional.ofNullable(Constant.class.getClassLoader().getResource(resourceFontPath)).map(URL::getPath).orElse(null);
            fontPath = URLDecoder.decode(fontPath, StandardCharsets.UTF_8);
            if (fontPath == null) {
                log.error("致命：无法找到默认字体文件");
                throw new RuntimeException("无法找到默认字体文件");
            }
            chineseFont = PdfFontFactory.createFont(fontPath+",0",  PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            if (chineseFont == null) {
                log.error("致命：无法创建默认字体");
                throw new RuntimeException("无法创建默认字体");
            }

            // 处理文本内容：按换行符分割，逐行添加到 PDF（保持原格式）
            String[] contentLines = content.split("\n"); // 按 \n 分割每行文本
            for (String line : contentLines) {
                Paragraph paragraph;
                // 识别标题（第一行"劳动合同"设为标题样式）
                if (line.equals("劳动合同")) {
                    paragraph = new Paragraph(line)
                            .setFont(chineseFont)
                            .setFontSize(16) // 标题字号
                            .setFontColor(new DeviceRgb(0, 51, 102)) // 深蓝色
                            .setTextAlignment(TextAlignment.CENTER) // 居中对齐
                            .setMarginBottom(20f); // 底部间距
                }
                // 识别章节标题（如"一、合同双方基本信息"）
                else if (line.matches("^[一二三四五六七八九十]+、.*")) {
                    paragraph = new Paragraph(line)
                            .setFont(chineseFont)
                            .setFontSize(12)
                            .setFontColor(new DeviceRgb(0, 77, 153)) // 中蓝色
                            .setMarginTop(15f) // 顶部间距
                            .setMarginBottom(8f);
                }
                // 普通文本（正文、填空行）
                else {
                    paragraph = new Paragraph(line)
                            .setFont(chineseFont)
                            .setFontSize(10)
                            .setMarginBottom(5f); // 行间距
                }
                // 将段落添加到文档
                document.add(paragraph);
            }
            log.info("PDF 生成成功！存储路径：{}", PDF_SAVE_PATH);
            return new ResourceMultipartFile(new FileSystemResource(PDF_SAVE_PATH), conversationId + ".pdf", "application/pdf", conversationId);
        } catch (IOException e) {
            log.error("PDF 生成失败：文件写入异常或字体加载异常！", e);
        } catch (Exception e) {
            log.error("PDF 生成失败：未知异常！", e);
        }
        return null;
    }

    private boolean extracted(String PDF_SAVE_PATH) {
        File pdfFile = new File(PDF_SAVE_PATH);
        File parent = pdfFile.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ok = parent.mkdirs(); // 递归创建父目录
            if (!ok) {
                log.error("无法创建目录：" + parent.getAbsolutePath());
                return false;
            }
        }
        return true;
    }
}
