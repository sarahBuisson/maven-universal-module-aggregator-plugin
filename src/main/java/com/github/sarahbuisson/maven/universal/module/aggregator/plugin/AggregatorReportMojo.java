package com.github.sarahbuisson.maven.universal.module.aggregator.plugin;

import org.apache.commons.collections.map.HashedMap;
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * use default template to resume all the modules. default = true
     */
    @Parameter(property = "useDefaultTemplate", defaultValue = "true")
    protected boolean useDefaultTemplate;


    /**
     * path to the template (.st format) of files who resume all the modules. optional
     * example:
     * <aggregateTemplatesPath>src/main/resources/report/index_html.st</aggregateTemplates>
     * <aggregateTemplatesPath>src/main/resources/report2</aggregateTemplates>
     */
    @Parameter(property = "aggregateTemplatesPath")
    private String aggregateTemplatesPath;


    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;


    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession mavenSession;

    private AggregateGenerator generator = new AggregateGenerator();


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
        if (project.getModules().isEmpty()) {
            this.getLog().debug(project + "no modules: ignore");
            return;
        }

        File reportsDirectory = new File(getOutputDirectory());

        Map<String, MavenProject> datasByModules = new HashMap<String, MavenProject>();
        for (MavenProject module : (List<MavenProject>) project.getCollectedProjects()) {
            if (!module.getPackaging().equals("pom")) {

                if (copyModules) {

                    File moduleDirectoryInTheParentTarget = new File(reportsDirectory.getAbsoluteFile(), module.getArtifactId());
                    if (!moduleDirectoryInTheParentTarget.exists()) {
                        moduleDirectoryInTheParentTarget.mkdir();
                    }

                    try {
                        File[] filesToCopy = new File(module.getBuild().getDirectory()).listFiles((FilenameFilter) new RegexFileFilter(this.filesToAggregateModulePath));
                        this.getLog().debug("copy - in :" + moduleDirectoryInTheParentTarget);
                        if (filesToCopy != null)
                            for (File fileToCopy : filesToCopy) {
                                this.getLog().debug("copy file:" + fileToCopy);
                                FileUtils.copyDirectory(fileToCopy, moduleDirectoryInTheParentTarget);
                            }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                datasByModules.put(module.getArtifactId(), module);
            }
        }

        Map<String, Object> attributes = new HashedMap();
        attributes.put("datasByModules", datasByModules);
        attributes.put("mojo", this);
        attributes.put("modules", datasByModules.values());

        if (aggregateTemplatesPath != null) {
            generator.createCustomAggregatorFiles(aggregateTemplatesPath, this.getOutputDirectory(), attributes);
        }
        if (useDefaultTemplate) {
            generator.createDefaultAggregatorFiles(this.getOutputDirectory(), attributes);
        }

        this.getLog().debug("Aggregator - ending");
    }


    @Override
    public boolean isExternalReport() {
        return true;
    }


}
