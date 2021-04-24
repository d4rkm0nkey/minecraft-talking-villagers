#!/usr/bin/python

import sys
import getopt
import os
import json
import re

def main(argv) :
    if(len(sys.argv) < 2):
        print("usage: python3 create_folder_structure.py [path]")
        raise AttributeError('No path specified')
    inputfolder = os.path.abspath(sys.argv[1])

    professions = [f.path for f in os.scandir(inputfolder) if f.is_dir()]
    sound_file = {}
    lang_file = {}
    config_file = {}
    print("Type in the identifier for your voice (name of the voice). This should be different from other voicepacks.")
    voicename = str(input())
    print("\n")
    print("Found categories:")
    config_file["name"] = voicename
    for profession_path in professions:
        dissembled_profession_path = profession_path.split("/")
        profession_name = dissembled_profession_path[-1]
        config_file[profession_name] = {}
        categories = [f.path for f in os.scandir(profession_path) if f.is_dir()]
        for category_path in categories:
            dissembled_category_path = category_path.split(os.sep)
            category_name = dissembled_category_path[-1]
            print("-", profession_name + ": " + category_name)
            config_file[profession_name][category_name] = []
            file_dict = load_files(category_path)
            for sound in file_dict:
                sound_id = voicename + "-" + sound
                config_file[profession_name][category_name].append(sound)
                subtitle_id = "subtitles.talkingvillagers." + voicename + "_" + sound
                lang_file[subtitle_id] = ""
                sound_file[sound_id] = {}
                sound_file[sound_id]["subtitle"] = subtitle_id
                sound_file[sound_id]["sounds"] = file_dict[sound]

    with open(os.path.join(inputfolder,'assets', 'talkingvillagers' ,'sounds.json'), 'w') as fp:
        json.dump(sound_file, fp, indent=4)

    with open(os.path.join(inputfolder, voicename + '.json'), 'w') as fp:
        json.dump(config_file, fp, indent=4)

    lang_folder = os.path.join(inputfolder,'assets', 'talkingvillagers', 'lang')
    os.mkdir(lang_folder)
    with open(os.path.join(lang_folder, 'en_us.json'), 'w') as fp:
        json.dump(lang_file, fp, indent=4)


def load_files(folderpath):
    files = {}
    path = os.path.normpath(folderpath)
    path = path.split(os.sep)
    for file in os.listdir(folderpath):
        if file.endswith(".ogg"):
            file = re.sub('.ogg', '', file)
            soundname = re.sub(r'[0-9]+', '', file)
            if(soundname not in files):
                files[soundname] = []
            filename = "talkingvillagers:voices/" + path[-3] + "/" + path[-2] + "/" + path[-1] + "/" + file
            files[soundname].append(filename)
    return files


if __name__ == "__main__":
   main(sys.argv[1:])
