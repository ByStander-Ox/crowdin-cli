package com.crowdin.cli.commands.actions;

import com.crowdin.cli.client.Client;
import com.crowdin.cli.client.Project;
import com.crowdin.cli.commands.functionality.ProjectFilesUtils;
import com.crowdin.cli.commands.functionality.RequestBuilder;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.utils.console.ConsoleSpinner;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcestrings.model.AddSourceStringRequest;

import java.util.Map;

import static com.crowdin.cli.BaseCli.RESOURCE_BUNDLE;
import static com.crowdin.cli.utils.console.ExecutionStatus.*;

public class StringAddAction implements Action {

    private final boolean noProgress;
    private final String text;
    private final String identifier;
    private final Integer maxLength;
    private final String context;
    private final String[] files;
    private final Boolean hidden;

    public StringAddAction(boolean noProgress, String text, String identifier, Integer maxLength, String context, String[] files, Boolean hidden) {
        this.noProgress = noProgress;
        this.text = text;
        this.identifier = identifier;
        this.maxLength = maxLength;
        this.context = context;
        this.files = files;
        this.hidden = hidden;
    }

    @Override
    public void act(PropertiesBean pb, Client client) {
        Project project;
        try {
            ConsoleSpinner.start(RESOURCE_BUNDLE.getString("message.spinner.fetching_project_info"), this.noProgress);
            project = client.downloadFullProject();
            ConsoleSpinner.stop(OK);
        } catch (Exception e) {
            ConsoleSpinner.stop(ERROR);
            throw new RuntimeException(RESOURCE_BUNDLE.getString("error.collect_project_info"), e);
        }

        if (files == null || files.length == 0) {
            AddSourceStringRequest request = RequestBuilder.addString(this.text, this.identifier, this.maxLength, this.context, null, this.hidden);
            client.addSourceString(request);
            System.out.println(OK.withIcon(RESOURCE_BUNDLE.getString("error.file_not_exists")));
        } else {
            Map<String, File> paths = ProjectFilesUtils.buildFilePaths(project.getDirectories(), project.getBranches(), project.getFiles());
            for (String file : files) {
                if (!paths.containsKey(file)) {
                    System.out.println(WARNING.withIcon(String.format(RESOURCE_BUNDLE.getString("error.file_not_exists"), file)));
                    continue;
                }
                Long fileId = paths.get(file).getId();

                AddSourceStringRequest request = RequestBuilder.addString(this.text, this.identifier, this.maxLength, this.context, fileId, this.hidden);
                client.addSourceString(request);
                System.out.println(OK.withIcon(String.format(RESOURCE_BUNDLE.getString("message.source_string_for_file_uploaded"), file)));
            }
        }

    }
}
