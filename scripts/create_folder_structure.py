import sys, os

categories = [
    "ambient",
    "celebrate",
    "death",
    "eat",
    "gossip",
    "greeting",
    "hero",
    "herodrop",
    "hurt",
    "level",
    "no",
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

def main(argv) :
    if(len(sys.argv) < 2):
        print("usage: python3 create_folder_structure.py [path]")
        raise AttributeError('No output path specified')
    outputfolder = os.path.abspath(sys.argv[1])

    for profession in professions:
        os.mkdir(os.path.join(outputfolder, profession))
        for category in categories:
            os.mkdir(os.path.join(outputfolder, profession, category))

if __name__ == "__main__":
   main(sys.argv[1:])