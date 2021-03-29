package io.slgl.template;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

class TemplateCompiler {

    private static final Pattern VERTICAL_WHITESPACE_PATTERN = Pattern.compile("\\v+");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("\\h*\\*\\h+(.*)");

    public Pattern compile(String templateText) {
        return compile(templateText, TemplateCompilerConfig.DEFAULT_CONFIG);
    }

    public Pattern compile(String templateText, TemplateCompilerConfig config) {
        String regex = readElements(templateText)
                .stream()
                .map(textElement -> textElement.toRegex(config))
                .collect(joiningRegexGroups());

        return Pattern.compile("\\s*(?:" + regex + ")\\s*");
    }

    private static Collector<CharSequence, ?, String> joiningRegexGroups() {
        return joining(")\\s?(?:", "(?:", ")");
    }


    private List<TextElement> readElements(String text) {
        List<TextElement> results = new ArrayList<>();
        List<String> currentList = null;

        String[] lines = VERTICAL_WHITESPACE_PATTERN.split(text);

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            Matcher listItemMatcher = LIST_ITEM_PATTERN.matcher(line);
            if (listItemMatcher.matches()) {
                String listItemText = listItemMatcher.group(1);
                if (currentList == null) {
                    currentList = new ArrayList<>();
                    results.add(new TextElement.ListElement(currentList));
                }
                currentList.add(listItemText);

            } else {
                currentList = null;
                results.add(new TextElement.ParagraphElement(line));
            }
        }

        return unmodifiableList(results);
    }

    interface TextElement {

        String toRegex(TemplateCompilerConfig template);

        class ParagraphElement implements TextElement {
            private final String text;

            public ParagraphElement(String text) {
                this.text = text;
            }

            @Override
            public String toRegex(TemplateCompilerConfig template) {
                String normalized = TextNormalizer.normalize(text);
                return Pattern.quote(normalized);
            }
        }

        class ListElement implements TextElement {
            private final List<String> items;

            public ListElement(List<String> items) {
                this.items = items;
            }

            @Override
            public String toRegex(TemplateCompilerConfig template) {
                return items.stream()
                        .map(TextNormalizer::normalize)
                        .map(Pattern::quote)
                        .map(quotedText -> template.getUnorderedListMarkerPattern() + quotedText)
                        .collect(joiningRegexGroups());
            }
        }
    }

}
