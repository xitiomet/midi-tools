package org.openstatic.midi;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.json.JSONObject;
import org.openstatic.MidiControlRule;
import org.openstatic.MidiTools;

public interface MidiToolsPlugin
{
    public JPanel getPanel();
    public String getTitle();
    public Icon getIcon();
    public JSONObject getSettings();
    public void loadSettings(MidiTools instance, JSONObject settings);
    public JSONObject getProject();
    public void loadProject(JSONObject project);
    public boolean onRule(MidiControlRule rule, String target, int old_value, int new_value);
    public Collection<String> getRuleTargets();
}