package com.wiseflow.core.parser;

import com.wiseflow.entity.Article;
import com.wiseflow.model.ParseRule;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public interface Parser {
    Article parse(Document doc, ParseRule rule);
    String extractTitle(Element doc, String selector);
    String extractContent(Element doc, String selector);
} 