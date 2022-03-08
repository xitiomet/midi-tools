package org.openstatic.midi;

import org.json.JSONObject;

public interface RandomizerRulesListener
{
    public void ruleAdded(int idx, JSONObject rule);
    public void ruleRemoved(int idx, JSONObject rule);
}
