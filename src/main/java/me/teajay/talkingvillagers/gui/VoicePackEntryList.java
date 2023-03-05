package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import me.teajay.talkingvillagers.voicepacks.Voicepack;

import java.util.ArrayList;
import java.util.function.Function;

public class VoicePackEntryList extends SpruceEntryListWidget<VoicePackEntry> {
    private int lastIndex = 0;
    public VoicePackEntryList(Position position, int width, int height, int anchorYOffset, Class<VoicePackEntry> entryClass) {
        super(position, width, height, anchorYOffset, entryClass);
    }

    public void add(ArrayList<Voicepack> voicepacks, Lambda callBack) {
        for(var voicepack : voicepacks) {
            this.addEntry(VoicePackEntry.create(voicepack, this, callBack));
        }
    }

    public int getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }
}
