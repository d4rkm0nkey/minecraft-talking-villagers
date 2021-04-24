#!/usr/bin/python

import sys
import getopt
import os
import json
import re


def main(argv) :
    inputfolder = ''
    outputfolder = ''
    try:
        opts, args = getopt.getopt(argv, "hi:o:", ["ifile=", "ofile="])
    except getopt.GetoptError:
        print('test.py -i <inputfolder> -o <outputfolder>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('test.py -i <inputfolder> -o <outputfolder>')
            sys.exit()
        elif opt in ("-i", "--ifile"):
            inputfolder = os.path.abspath(arg)
        elif opt in ("-o", "--ofile"):
            outputfolder = arg

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

    with open(os.path.join(outputfolder,'sounds.json'), 'w') as fp:
        json.dump(sound_file, fp, indent=4)

    with open(os.path.join(outputfolder, voicename + '.json'), 'w') as fp:
        json.dump(config_file, fp, indent=4)

    with open(os.path.join(outputfolder, 'en_us.json'), 'w') as fp:
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
