package com.felixzz.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.felixzz.dialog.MvnProjectVersionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.actions.MavenAction;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;

/**
 * @author Felix
 * @date 2020/8/18 11:17
 */
public class MvnProjectVersionAction extends MavenAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        MvnProjectVersionDialog formDialog = new MvnProjectVersionDialog(e);
        formDialog.setResizable(false);
        formDialog.show();
    }

    @Override
    protected boolean isAvailable(@NotNull AnActionEvent e) {
        if (!super.isAvailable(e)) {
            return false;
        }
        final MavenProjectsManager projectsManager = MavenActionUtil.getProjectsManager(e.getDataContext());
        return projectsManager != null && projectsManager.isMavenizedProject();
    }

}
