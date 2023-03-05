package me.teajay.talkingvillagers.voicepacks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.io.VoiceInfo;
import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoicepackInfo;

import java.io.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VoicepackReader {
    public static final Path VOICE_INSTALLATION_PATH = Path.of(TalkingVillagers.CONFIG_DIRECTORY.toString(), "voices");
    public static final String VOICE_PATH = "assets/talkingvillagers/sounds/voices/";
    public static final String SOUND_FILE_NAME = "assets/talkingvillagers/sounds.json";
    private final File voicepackFile;
    private Voicepack voicepack;
    private final ArrayList<String> installedVoices = new ArrayList<>();
    private final ArrayList<SoundEntry> sounds = new ArrayList<>();

    public VoicepackReader(File file) {
        this.voicepackFile = file;
        getInstalledVoices();
        loadVoicedata();
    }

    public boolean hasVoicepack() {
        return voicepack != null;
    }

    public Voicepack getVoicepack() {
        return voicepack;
    }

    private ArrayList<String> getInstalledVoices() {
        File voiceInstallationDir = VOICE_INSTALLATION_PATH.toFile();

        for(File voiceConfig : voiceInstallationDir.listFiles(path -> path.getPath().endsWith(".json"))) {
            String identifier = voiceConfig.getName().replace(".json", "");
            installedVoices.add(identifier);
        }
        return installedVoices;
    }


    private void loadVoicedata() {
        voicepack = null;
        VoicepackInfo info = null;
        ZipFile file = null;

        List<VoiceInfo> voiceInfos = new ArrayList<>();
        try {
            file = new ZipFile(this.voicepackFile);

            final Enumeration<? extends ZipEntry> entries = file.entries();
            while ( entries.hasMoreElements() )
            {
                final ZipEntry entry = entries.nextElement();
                if(entry.getName().equals("voicepack.json")) {
                    Gson gson = new Gson();
                    Reader reader = new InputStreamReader(file.getInputStream(entry));
                    info = gson.fromJson(reader, VoicepackInfo.class);
                } else if(entry.getName().equals(SOUND_FILE_NAME)) {
                    addSound(file, entry);
                } else if(entry.getName().startsWith(VOICE_PATH)) {
                    addVoice(file, voiceInfos, entry);
                }
            }
        }
        catch (IOException e) {
            TalkingVillagers.LOGGER.error(e);
        }
        finally
        {
            try {
                file.close();
            } catch (IOException e) {
                TalkingVillagers.LOGGER.error(e);
            }
        }

        createVoicepack(info, voiceInfos);
    }

    private void addSound(ZipFile file, ZipEntry entry) throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream(entry));
        JsonElement parsed = JsonParser.parseReader(reader);
        Gson gson = new Gson();
        for (var element: parsed.getAsJsonObject().entrySet()) {
            var sound = gson.fromJson(element.getValue().getAsJsonObject(), SoundEntry.class);
            sound.setVoiceId(element.getKey().split("-")[0]);
             sounds.add(sound);
        }
    }

    private void createVoicepack(VoicepackInfo info, List<VoiceInfo> voiceInfos) {
        if(info != null) {
            voicepack = new Voicepack(info.getId(), info.getName(), info.getVersion());
            voiceInfos.sort((voiceInfo, t1) -> voiceInfo.getId().compareToIgnoreCase(t1.getId()));
            for (var voice : voiceInfos) {
                voicepack.addVoice(new VoiceFile(voice.getId(), voice.getName(), voice.getType(), installedVoices.contains(voice.getId())));
            }
            voicepack.setSounds(sounds);
        }
    }

    private void addVoice(ZipFile file, List<VoiceInfo> voiceInfos, ZipEntry entry) throws IOException {
        Gson gson = new Gson();
        String path = entry.getName().replace(VOICE_PATH, "");
        var deconstructPath = path.split("/");
        String voicename = deconstructPath[0];
        if(entry.getName().equals(VOICE_PATH + voicename + "/" + voicename + ".json")) {
            Reader reader = new InputStreamReader(file.getInputStream(entry));
            var voice = gson.fromJson(reader, VoiceInfo.class);
            if(voice != null) voiceInfos.add(voice);
        }
    }
}
