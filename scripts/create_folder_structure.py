import sys, os, json

categories = [
    "ambient",
    "celebrate",
    "death",
    "gossip",
    "greeting",
    "hero",
    "herodrop",
    "hurt",
    "level",
    "nope",
    "random",
    "trade",
    "trade_success",
    "good_weather",
    "bad_weather",
    "yes"
]

professions = [
    "all",
    "nitwit",
    "farmer",
    "librarian",
    "cartographer",
    "toolsmith",
    "fletcher",
    "shepherd",
    "leatherworker",
    "mason",
    "fisherman",
    "cleric",
    "armorer",
    "butcher",
    "weaponsmith"
]

pre_path = "assets/talkingvillagers/sounds/voices"

pack_info = {
    "pack": {
        "description": "",
        "pack_format": 6
    }
}

def main(argv) :
    if(len(sys.argv) < 2):
        print("usage: python3 create_folder_structure.py [path]")
        raise AttributeError('No output path specified')
    outputfolder = os.path.abspath(sys.argv[1])
    print("Type in the identifier for your voice (name of the voice). This should be different from other voicepacks.")
    voicename = str(input())
    pack_info["pack"]["description"] = "Talking Villagers: " + voicename + " v1.0"
    with open(os.path.join(outputfolder,'pack.mcmeta'), 'w') as fp:
        json.dump(pack_info, fp, indent=4)
    path = os.path.join(outputfolder, pre_path, voicename)
    for profession in professions:
        os.makedirs(os.path.join(path, profession))
        for category in categories:
            os.mkdir(os.path.join(path, profession, category))

if __name__ == "__main__":
   main(sys.argv[1:])