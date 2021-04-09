package com.felixzz.dialog;

import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.awt.*;

/**
 * @author Felix
 * @date 2021/4/2 23:39
 */
public class CustomRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        MavenProject item = (MavenProject) value;
        String str = item.getMavenId().getGroupId() + ":" + item.getMavenId().getArtifactId();
        return super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
    }
}
