/*
 * Copyright 2015 Jason Fehr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.sarahbuisson.maven.universal.module.aggregator.plugin;

import com.google.common.io.Files;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Merge the report of the pit mutation testing from the module to the parent.
 */
@Mojo(name = "aggregate", defaultPhase = LifecyclePhase.PRE_SITE)
public class AggregatorReportMojo extends AbstractMavenReport {

    @Component
    private Renderer siteRenderer;

    @Parameter(property = "site.report.description", defaultValue = "aggregations of multiple modules")
    String siteReportDescription;

    @Parameter(property = "site.report.name", defaultValue = "Aggregation Report")
    private String siteReportName;


    /**
     * Base directory where all pit reports are written to by the mutationCoverage
     * goal. If timestampedReports is true (the default), then the actual reports
     * will be contained in a subdirectory within this directory. If
     * timestampedReports is false, the actual reports will be in this directory.
     * <p>
     * if empty, take the value of filesToAggregateModulePath
     */
    @Parameter(property = "reportsDirectory")
    private String reportsDirectory;

    /**
     * relative path, in a module, to the files who will-be aggregate in the parent.
     * regexp are allowed.
     */
    @Parameter(property = "filesToAggregateModulePath", required = true)
    protected String filesToAggregateModulePath;


    /**
     * copy the files of the modules into the parent's target.
     */
    @Parameter(property = "copyModules", defaultValue = "true")
    protected boolean copyModules;


    /**
     * path to the template (.st format) of files who resume all the modules. optional
     * example:
     * <aggregateTemplates>index.st</aggregateTemplates>
     */
    @Parameter(property = "aggregateTemplates")
    private File[] aggregateTemplate;


    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;


    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession mavenSession;


    public AggregatorReportMojo() {
        super();
    }

    @Override
    protected Renderer getSiteRenderer() {
        return this.siteRenderer;
    }

    @Override
    protected String getOutputDirectory() {
        return this.project.getBuild().getDirectory() + File.separator + this.getReportsDirectory();
    }


    public String getOutputName() {
        return this.getReportsDirectory() + File.separator + "index";
    }

    public String getName(Locale locale) {
        return this.siteReportName;
    }

    public String getDescription(Locale locale) {
        return siteReportDescription;
    }

    @Override
    protected MavenProject getProject() {
        return this.project;
    }

    /**
     * Getter for property 'reportsDirectory'.
     *
     * @return Value for property 'reportsDirectory'.
     */
    public String getReportsDirectory() {
        if (reportsDirectory == null) {
            reportsDirectory = filesToAggregateModulePath;
        }
        return reportsDirectory;
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        this.getLog().debug("Aggregator - starting");
        if (!project.getPackaging().equals("pom")) {
            this.getLog().debug(project + "not a pom project: ignore");
            return;
        }
        for (File f : aggregateTemplate)
            if (f.isDirectory()) {
                List<File> templates = new ArrayList<File>();
                templates.addAll(Arrays.asList(aggregateTemplate));
                templates.remove(f);
                templates.addAll(Arrays.asList(f.listFiles()));
                aggregateTemplate =templates.toArray(new File[]{});
            }
        File reportsDirectory = new File(getOutputDirectory());

        Map<String, MavenProject> datasByModules = new HashMap<String, MavenProject>();
        for (MavenProject module : (List<MavenProject>) project.getCollectedProjects()) {
            if (!module.getPackaging().equals("pom")) {

                if (copyModules) {

                    File moduleDirectoryInTheParentTarget = new File(reportsDirectory.getAbsoluteFile(), module.getArtifactId());

                    moduleDirectoryInTheParentTarget.mkdir();


                    try {
                        File[] filesToCopy = new File(module.getBuild().getDirectory()).listFiles((FilenameFilter) new RegexFileFilter(this.filesToAggregateModulePath));
                        for (File fileToCopy : filesToCopy) {
                            this.getLog().debug("copy - starting:" + fileToCopy);
                            this.getLog().debug("copy - in :" + moduleDirectoryInTheParentTarget);
                            FileUtils.copyDirectory(fileToCopy, moduleDirectoryInTheParentTarget);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                datasByModules.put(module.getArtifactId(), module);
            }
        }

        if(aggregateTemplate ==null || aggregateTemplate.length==0) {
            createDefaultAggregatorFiles(datasByModules);
        }else {
            createCustomAggregatorFiles(datasByModules);
        }

        this.getLog().debug("Aggregator - ending");
    }


    @Override
    public boolean isExternalReport() {
        return true;
    }


    private void createDefaultAggregatorFiles(Map<String, MavenProject> indexByModules) {

        StringTemplateGroup group = new StringTemplateGroup("aggregator");


        try {
            final Writer writerIndex;
            writerIndex = createWriterForFile(this.getOutputDirectory() + File.separator + "index.html");
            final StringTemplate stIndex = group
                    .getInstanceOf("maven-universal-module-aggregator-plugin/templates/index");
            writerIndex.write(computeStringTemplate(stIndex, indexByModules));
            writerIndex.close();


            final Writer writerCss;
            writerCss = createWriterForFile(this.getOutputDirectory() + File.separator + "style.css");
            final StringTemplate stCss = group
                    .getInstanceOf("maven-universal-module-aggregator-plugin/templates/style");
            writerCss.write(computeStringTemplate(stCss, indexByModules));
            writerCss.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createCustomAggregatorFiles(Map<String, MavenProject> indexByModules) {
        for (File template : aggregateTemplate) {

            try {


                String fileName = computeStringTemplate(new StringTemplate(Files.getNameWithoutExtension(template.getName())), indexByModules);

                final Writer writer = createWriterForFile(this.getOutputDirectory() + File.separator + fileName);
                final StringTemplate st = new StringTemplate(Files.toString(template, Charset.defaultCharset()));
                writer.write(computeStringTemplate(st, indexByModules));
                writer.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    String computeStringTemplate(StringTemplate st, Map<String, MavenProject> indexByModules) {

        st.setAttribute("indexByModules", indexByModules);
        st.setAttribute("modules", indexByModules.values());
        st.setAttribute("mojo", this);
        return st.toString();


    }

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

}
