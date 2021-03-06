package com.crowdin.cli.commands.actions;

import com.crowdin.cli.client.Client;
import com.crowdin.cli.client.Project;
import com.crowdin.cli.commands.functionality.ProjectFilesUtils;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.utils.console.ConsoleSpinner;
import com.crowdin.client.sourcefiles.model.File;
import com.crowdin.client.sourcestrings.model.SourceString;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.crowdin.cli.BaseCli.RESOURCE_BUNDLE;
import static com.crowdin.cli.utils.console.ExecutionStatus.*;

public class StringListAction implements Action {

    private final boolean noProgress;
    private final boolean isVerbose;
    private final String file;
    private final String filter;

    public StringListAction(boolean noProgress, boolean isVerbose, String file, String filter) {
        this.noProgress = noProgress;
        this.isVerbose = isVerbose;
        this.file = file;
        this.filter = filter;
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

        Map<String, File> paths = ProjectFilesUtils.buildFilePaths(project.getDirectories(), project.getBranches(), project.getFiles());
        Map<Long, String> reversePaths = paths.entrySet()
            .stream()
            .collect(Collectors.toMap((entry) -> entry.getValue().getId(), Map.Entry::getKey));

        String encodedFilter;
        try {
            encodedFilter = (filter != null) ? URLEncoder.encode(filter, StandardCharsets.UTF_8.toString()) : null;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        List<SourceString> sourceStrings;
        if (StringUtils.isEmpty(file)) {
            sourceStrings = client.listSourceString(null, encodedFilter);
        } else {
            if (paths.containsKey(file)) {
                sourceStrings = client.listSourceString(paths.get(file).getId(), encodedFilter);
            } else {
                throw new RuntimeException(String.format(RESOURCE_BUNDLE.getString("error.file_not_exists"), file));
            }
        }
        if (sourceStrings.isEmpty()) {
            System.out.println(WARNING.withIcon(RESOURCE_BUNDLE.getString("message.source_string_list_not_found")));
        }
        sourceStrings.forEach(ss -> {
            System.out.println(String.format(RESOURCE_BUNDLE.getString("message.source_string_list_text"), ss.getId(), ss.getText()));
            if (isVerbose) {
                if (ss.getContext() != null) {
                    System.out.println(String.format(RESOURCE_BUNDLE.getString("message.source_string_list_context"), ss.getContext().trim().replaceAll("\n", "\n\t\t")));
                }
                if (ss.getFileId() != null) {
                    System.out.println(String.format(RESOURCE_BUNDLE.getString("message.source_string_list_file"), reversePaths.get(ss.getFileId())));
                }
                if (ss.getMaxLength() != null && ss.getMaxLength() != 0) {
                    System.out.println(String.format(RESOURCE_BUNDLE.getString("message.source_string_list_max_length"), ss.getMaxLength()));
                }
            }
        });
    }
}
