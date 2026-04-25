package newblock.chatlog;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理违禁词替换配置
 */
public class FilterReplaceConfig {
    private final Logger logger;
    private final File configFile;
    private List<String> replacePatterns;

    /**
     * 创建违禁词替换配置管理器
     *
     * @param logger 日志记录器
     * @param pluginDir 插件目录
     */
    public FilterReplaceConfig(Logger logger, File pluginDir) {
        this.logger = logger;
        this.configFile = new File(pluginDir, "filter_replace.yml");
        this.replacePatterns = new ArrayList<>();
        createDefaultConfig();
        loadConfig();
    }

    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        if (!configFile.exists()) {
            String defaultConfig = ""
                    + "# ChatLog 违禁词替换配置\n"
                    + "# 每行一个违禁词正则表达式\n"
                    + "# 空行和以#开头的行会被忽略\n"
                    + "# 注意：检测到违禁词后，整段话将被替换为 config.yml 中配置的 ReplaceWith 字符串\n"
                    + "# 示例：\n"
                    + "cnm\n";

            try {
                Files.write(configFile.toPath(), defaultConfig.getBytes());
                logger.info("已生成默认 filter_replace.yml");
            } catch (IOException e) {
                logger.error("创建默认 filter_replace.yml 时出错", e);
            }
        }
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        replacePatterns.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 忽略空行或注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 添加违禁词正则表达式
                try {
                    // 预编译检查正则表达式的有效性
                    Pattern.compile(line, Pattern.CASE_INSENSITIVE);
                    replacePatterns.add(line);
                } catch (PatternSyntaxException e) {
                    logger.error("无效的替换正则表达式: {}", line, e);
                }
            }
            
            logger.info("已加载 {} 个有效的替换正则表达式", replacePatterns.size());
        } catch (IOException e) {
            logger.error("读取 filter_replace.yml 时发生错误", e);
        }
    }

    /**
     * 获取替换模式列表
     */
    public List<String> getReplacePatterns() {
        return replacePatterns;
    }
}
