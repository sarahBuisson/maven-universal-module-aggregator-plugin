package com.github.sarahbuisson.maven.universal.module.aggregator.plugin;

import com.google.common.io.Files;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.*;
import java.util.*;

/**
 * Created by sbuisson on 21/11/2017.
 */
public class AggregateGenerator {


    public static final  List<String> DEFAULT_TEMPLATE=Arrays.asList("templates/report/index_html","templates/report/style_css");

    public void createDefaultAggregatorFiles(String outputDirectory, Map<String, Object> attributes) {

        StringTemplateGroup group = new StringTemplateGroup("templates");
        List<StringTemplate> templates = new ArrayList<StringTemplate>();
        for (String template : DEFAULT_TEMPLATE)
            templates.add(group.getInstanceOf(template));
        createCustomAggregatorFilesFromTemplates(templates,outputDirectory, "templates/report/", attributes);

    }

    public void createCustomAggregatorFiles(String templatesPath, String outputDirectory, Map<String, Object> attributes) {

        File templateFile = new File(templatesPath);
        if (templateFile.isDirectory()) {
            StringTemplateGroup group = new StringTemplateGroup("templates", templatesPath);
            List<StringTemplate> templates = new ArrayList<StringTemplate>();
            for (File template : templateFile.listFiles())
                templates.add(group.getInstanceOf(Files.getNameWithoutExtension(template.getName())));
            createCustomAggregatorFilesFromTemplates(templates, outputDirectory, templatesPath, attributes);
        } else

        {
            createCustomAggregatorFilesFromTemplates(Arrays.asList(new StringTemplate(templatesPath)), outputDirectory, templatesPath, attributes);
        }
    }

    public void createCustomAggregatorFilesFromTemplates(Collection<StringTemplate> aggregateTemplates, String outputDirectory, String pathToRemove, Map<String, Object> attributes) {

        for (StringTemplate template : aggregateTemplates) {


            try {


                String fileName = replaceLast(template.getName(),"_",".");

                final Writer writer = createWriterForFile(outputDirectory + File.separator + fileName.replace(pathToRemove,""));
                template.setAttributes(attributes);

                writer.write(template.toString());
                writer.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
/*
    private Collection<String> replaceDirectoryByFiles(Collection<String> aggregateTemplatesPath, String root) {
        List<String> templates = new ArrayList<String>();
        for (String path : aggregateTemplatesPath) {
            File f = new File(path);
            if (f.isDirectory()) {
                List<String> childrens = new ArrayList<String>();
                for (File child : f.listFiles())

                        childrens.add(child.getPath());

                templates.addAll(replaceDirectoryByFiles(childrens, root+File.separator+f.getName()));
                aggregateTemplatesPath = templates;
            }
            else{
                templates.add(root+" "+)
            }
        }
        return templates;
    }
*/

    public Writer createWriterForFile(final String filePath) throws IOException {

        final int fileSepIndex = filePath.lastIndexOf(File.separatorChar);
        if (fileSepIndex > 0) {

            final File directoryFile = new File(filePath).getParentFile();
            if (!directoryFile.exists()) {
                directoryFile.mkdirs();
            }
        }
        return new BufferedWriter(new FileWriter(filePath));

    }

    String replaceLast(String string, String substring, String replacement)
    {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }
}
