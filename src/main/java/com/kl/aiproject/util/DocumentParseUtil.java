package com.kl.aiproject.util;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Locale;

@Component
public class DocumentParseUtil {

    public List<Document> parse(String filePath){
        File file = new File(filePath);
        String suffix = filePath.substring(filePath.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        Resource resource = new FileSystemResource(file);

        DocumentReader reader = switch(suffix){
            case "txt","text" -> new TextReader(resource);
            case "md" -> new MarkdownDocumentReader(resource,
                    MarkdownDocumentReaderConfig.builder()
                            .withAdditionalMetadata("source", file.getName())
                            .build());
            default -> throw new IllegalArgumentException("不支持的文件格式"+suffix);
        };
        return reader.get();
    }
}
