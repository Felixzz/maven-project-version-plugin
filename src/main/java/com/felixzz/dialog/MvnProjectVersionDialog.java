package com.felixzz.dialog;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.model.MavenDomDependencies;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;

import javax.swing.*;
import java.util.*;

/**
 * @author Felix
 * @date 2020/8/18 15:53
 */
public class MvnProjectVersionDialog extends DialogWrapper {

    private final Project project;

    private final MavenProjectsManager projectsManager;

    private final List<MavenProject> allProjects;

    private final List<MavenProject> rootProjects;

    private MavenProject rootProject;

    private JTextField newVersionContent;

    public MvnProjectVersionDialog(AnActionEvent e) {
        super(e.getProject());
        setTitle("Set the Project Version");
        this.project = e.getProject();
        projectsManager = MavenActionUtil.getProjectsManager(e.getDataContext());
        allProjects = projectsManager == null ? new ArrayList<>() : projectsManager.getProjects();
        rootProjects = projectsManager == null ? new ArrayList<>() : projectsManager.getRootProjects();
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, -1));
        JLabel label = new JLabel("Root Project:");
        dialogPanel.add(label, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        ComboBox<MavenProject> comboBox = new ComboBox<>();
        comboBox.setRenderer(new CustomRenderer());
        dialogPanel.add(comboBox, new GridConstraints(0, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        JLabel newVersion = new JLabel("New Version:");
        newVersionContent = new JTextField();
        dialogPanel.add(newVersion, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        dialogPanel.add(newVersionContent, new GridConstraints(1, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        flushComboBox(comboBox);
        comboBox.addActionListener(e -> {
            Object o = comboBox.getSelectedItem();
            if (o != null) {
                rootProject = (MavenProject) o;
                initVersionField();
            }
        });
        SwingUtilities.invokeLater(() -> newVersionContent.requestFocusInWindow());
        return dialogPanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel south = new JPanel();
        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> WriteCommandAction.runWriteCommandAction(project, this::updateVersion));
        submit.setHorizontalAlignment(SwingConstants.CENTER);
        submit.setVerticalAlignment(SwingConstants.CENTER);
        south.add(submit);
        return south;
    }

    private void initVersionField() {
        VirtualFile file = rootProject.getFile();
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        DomManager domManager = DomManager.getDomManager(project);
        DomFileElement<MavenDomProjectModel> fileElement = domManager.getFileElement((XmlFile) psiFile, MavenDomProjectModel.class);
        if (fileElement != null) {
            MavenDomProjectModel rootElement = fileElement.getRootElement();
            GenericDomValue<String> version = rootElement.getVersion();
            newVersionContent.setText(version.getValue());
        }
    }

    private void flushComboBox(ComboBox<MavenProject> comboBox) {
        for (MavenProject mavenProject : rootProjects) {
            comboBox.addItem(mavenProject);
        }
        if (!rootProjects.isEmpty()) {
            rootProject = rootProjects.get(0);
            initVersionField();
        }
    }

    private void updateVersion() {
        final String newVersion = newVersionContent.getText();
        DomManager domManager = DomManager.getDomManager(project);
        Collection<String> allModuleNames = getAllModuleNames();
        try {
            if (!newVersion.isEmpty()) {
                for (MavenProject mavenProject : allProjects) {
                    VirtualFile file = mavenProject.getFile();
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (mavenProject.equals(rootProject)) {
                        DomFileElement<MavenDomProjectModel> fileElement = domManager.getFileElement((XmlFile) psiFile, MavenDomProjectModel.class);
                        if (fileElement != null) {
                            MavenDomProjectModel rootElement = fileElement.getRootElement();
                            GenericDomValue<String> version = rootElement.getVersion();
                            version.setValue(newVersion);
                            updateDependencyVersion(rootElement, newVersion, allModuleNames);
                        }
                    } else {
                        XmlFile xmlFile = (XmlFile) psiFile;
                        if (xmlFile != null) {
                            updateModuleVersion(newVersion, xmlFile);
                        }
                        DomFileElement<MavenDomProjectModel> fileElement = domManager.getFileElement(xmlFile, MavenDomProjectModel.class);
                        if (fileElement != null) {
                            MavenDomProjectModel rootElement = fileElement.getRootElement();
                            updateDependencyVersion(rootElement, newVersion, allModuleNames);
                        }
                    }
                }
                try {
                    FileDocumentManager.getInstance().saveAllDocuments();
                    projectsManager.forceUpdateProjects(allProjects);
                } catch (Exception e) {
                    FileDocumentManager.getInstance().saveAllDocuments();
                    projectsManager.forceUpdateProjects(allProjects);
                }
            }
        } catch (Exception ignored) {

        }
        this.close(DialogWrapper.OK_EXIT_CODE);
    }

    private Collection<String> getAllModuleNames(){
        Set<String> result = new HashSet<>();
        for (MavenProject mavenProject : allProjects) {
            result.addAll(mavenProject.getModulesPathsAndNames().values());
        }
        return result;
    }

    private void updateDependencyVersion(MavenDomProjectModel mavenDomProjectModel, String newVersion, Collection<String> moduleNames) {
        updateDependencyVersion(mavenDomProjectModel.getDependencies(), newVersion, moduleNames);
        updateDependencyVersion(mavenDomProjectModel.getDependencyManagement().getDependencies(), newVersion, moduleNames);
    }

    private void updateDependencyVersion(MavenDomDependencies dependencies, String newVersion, Collection<String> moduleNames) {
        List<MavenDomDependency> dependencyList = dependencies.getDependencies();
        for (MavenDomDependency dependency : dependencyList) {
            if (Objects.equals(dependency.getGroupId().getRawText(), rootProject.getMavenId().getGroupId()) &&
                    moduleNames.contains(dependency.getArtifactId().getRawText())
                    && dependency.getVersion().getValue() != null) {
                dependency.getVersion().setValue(newVersion);
            }
        }
    }

    private void updateModuleVersion(String newVersion, XmlFile xmlFile) {
        final XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            XmlTag parentTag = rootTag.findFirstSubTag("parent");
            if (parentTag != null) {
                XmlTag versionTagFromParent = parentTag.findFirstSubTag("version");
                if (versionTagFromParent != null) {
                    versionTagFromParent.getValue().setText(newVersion);
                }
            }
            XmlTag versionTag = rootTag.findFirstSubTag("version");
            if (versionTag != null) {
                versionTag.getValue().setText(newVersion);
            }
        }
    }
}