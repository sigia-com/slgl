package io.slgl.template;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import java.io.IOException;
import java.io.Writer;

public class FreemarkerTemplateRenderer {

    public void process(String templateText, Object data, Writer output) {
        try {
            getFreemarkerTemplate(templateText).process(data, output);
        } catch (TemplateException | IOException e) {
            throw new io.slgl.template.TemplateException(e.getMessage());
        }
    }

    public void validate(String text) {
        getFreemarkerTemplate(text);
    }

    private Template getFreemarkerTemplate(String templateText) {
        Template template;
        try {
            Configuration conf = new Configuration(new Version(2, 3, 29));
            conf.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
            conf.setInterpolationSyntax(Configuration.DOLLAR_INTERPOLATION_SYNTAX);

            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", templateText);
            conf.setTemplateLoader(stringTemplateLoader);

            template = conf.getTemplate("template");

        } catch (ParseException e) {
            throw new io.slgl.template.TemplateException(e.getMessage());
        } catch (IOException e) {
            throw new io.slgl.template.TemplateException("Unknown error");
        }
        return template;
    }
}
