package newblock.chatlog;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理违禁词过滤器
 */
public class FilterManager {
    private final Logger logger;
    private final ChatlogConfig config;
    private List<Pattern> forbiddenPatterns;
    
    // 替换相关
    private List<Pattern> replacePatterns;
    private String replaceWith;
    private boolean hasReplaceConfig;

    /**
     * 创建过滤器管理器
     *
     * @param logger 日志记录器
     * @param config 配置管理器
     */
    public FilterManager(Logger logger, ChatlogConfig config) {
        this.logger = logger;
        this.config = config;
        this.forbiddenPatterns = new ArrayList<>();
        this.replacePatterns = new ArrayList<>();
        this.replaceWith = "***";
        this.hasReplaceConfig = false;
        loadFilters();
    }

    /**
     * 从filter.yml加载违禁词正则表达式
     */
    public void loadFilters() {
        forbiddenPatterns = new ArrayList<>();
        File filterFile = config.getFilterFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(filterFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略空行或注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    Pattern p = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
                    forbiddenPatterns.add(p);
                } catch (Exception ex) {
                    logger.error("无法编译正则: {}，跳过此行。错误: {}", line, ex.getMessage());
                }
            }
            logger.info("已加载 {} 个违禁词正则", forbiddenPatterns.size());
        } catch (IOException e) {
            logger.error("读取 filter.yml 时发生错误，违禁词列表可能为空", e);
        }
    }

    /**
     * 设置替换配置
     * 
     * @param replaceConfig 替换配置
     */
    public void setReplaceConfig(FilterReplaceConfig replaceConfig) {
        if (replaceConfig == null) {
            this.hasReplaceConfig = false;
            return;
        }
        
        this.hasReplaceConfig = true;
        this.replaceWith = config.getReplaceWith();
        this.replacePatterns.clear();
        
        for (String pattern : replaceConfig.getReplacePatterns()) {
            try {
                replacePatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                logger.error("编译替换正则表达式失败: {}", pattern, e);
            }
        }
        
        logger.info("已加载 {} 个替换正则表达式，替换字符串: {}", replacePatterns.size(), replaceWith);
    }

    /**
     * 检查文本是否包含违禁词
     *
     * @param text 要检查的文本
     * @return 如果包含违禁词返回true，否则返回false
     */
    public boolean containsForbiddenWords(String text) {
        for (Pattern pattern : forbiddenPatterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 替换文本中的违禁词
     * 注意：如果检测到违禁词，整段话将被替换为配置的字符串
     *
     * @param text 原始文本
     * @return 替换后的文本，如果没有替换则返回null
     */
    public String replaceFilteredWords(String text) {
        if (!hasReplaceConfig || replacePatterns.isEmpty()) {
            return null;
        }
        
        // 检查是否包含任何违禁词
        for (Pattern pattern : replacePatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                // 只要检测到违禁词，就将整段话替换为配置的字符串
                return replaceWith;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否有替换配置
     *
     * @return 是否有替换配置
     */
    public boolean hasReplaceConfig() {
        return hasReplaceConfig;
    }

    /**
     * 获取违禁词正则表达式列表
     *
     * @return 违禁词正则表达式列表
     */
    public List<Pattern> getForbiddenPatterns() {
        return forbiddenPatterns;
    }
}
