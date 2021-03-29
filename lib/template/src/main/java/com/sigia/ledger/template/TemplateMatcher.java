package io.slgl.template;

import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

public class TemplateMatcher {

    private final FreemarkerTemplateRenderer templateRenderer = new FreemarkerTemplateRenderer();
    private final TemplateCompiler templateCompiler = new TemplateCompiler();
    private final Template template;

    public TemplateMatcher(Template template) {
        this.template = template;
    }

    public boolean isMatching(String documentText, Map<String, ?> requestObject) {
        try {
            String renderedTemplate = renderTemplate(requestObject);
            String normalizedText = TextNormalizer.normalize(documentText);
            return templateCompiler
                    .compile(renderedTemplate, new TemplateCompilerConfig(template))
                    .asPredicate()
                    .test(normalizedText);

        } catch (NoAllKeysAccessedException e) {
            return false;
        }
    }

    public String renderTemplate(Map<String, ?> context) throws NoAllKeysAccessedException {

        AccessCheckingMap dataModel = new AccessCheckingMap(context)
                .ignoreKeys("@link", "@id", "@type")
                .ignoreKeys(it -> it.startsWith("#"));

        StringWriter output = new StringWriter();
        templateRenderer.process(template.getText(), dataModel, output);

        Set<String> notAccessedKeys = dataModel.getNotAccessedKeys();
        if (!notAccessedKeys.isEmpty()) {
            throw new NoAllKeysAccessedException(notAccessedKeys);
        }

        return output.toString();
    }

    public static class NoAllKeysAccessedException extends Exception {

        private final Set<String> notAccessedKeys;

        public NoAllKeysAccessedException(Set<String> notAccessedKeys) {
            this.notAccessedKeys = notAccessedKeys;
        }

        public Set<String> getNotAccessedKeys() {
            return notAccessedKeys;
        }
    }

}
