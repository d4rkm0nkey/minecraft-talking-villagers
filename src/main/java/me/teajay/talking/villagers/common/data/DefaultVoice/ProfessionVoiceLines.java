package me.teajay.talking.villagers.common.data.DefaultVoice;

public class ProfessionVoiceLines {
    String[] trade;
    String[] trade_success;
    String[] yes;
    String[] hero;
    String[] greeting;
    String[] ambient;
    String[] level;
    String[] celebrate;
    String[] herodrop;
    String[] nope;
    String[] gossip;
    String[] random;
    String[] death;
    String[] hurt;
    String[] good_weather;
    String[] bad_weather;

    public ProfessionVoiceLines(String[] trade, String[] trade_success, String[] yes, String[] hero, String[] greeting, String[] ambient, String[] level, String[] celebrate, String[] herodrop, String[] nope, String[] gossip, String[] random, String[] death, String[] hurt, String[] good_weather, String[] bad_weather) {
        this.trade = trade;
        this.trade_success = trade_success;
        this.yes = yes;
        this.hero = hero;
        this.greeting = greeting;
        this.ambient = ambient;
        this.level = level;
        this.celebrate = celebrate;
        this.herodrop = herodrop;
        this.nope = nope;
        this.gossip = gossip;
        this.random = random;
        this.death = death;
        this.hurt = hurt;
        this.good_weather = good_weather;
        this.bad_weather = bad_weather;
    }

    public static ProfessionVoiceLines farmer() {
        return new ProfessionVoiceLines(
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{},
            new String[]{
                "good_season",
                "rain_enough",
                "look_for_field"
            },
            new String[]{},
            new String[]{},
            new String[]{
                "bad_weather_good",
                "rain_grow",
                "rain_happy"
            },
            new String[]{
                "sunny_day",
                "needed_rain",
                "plants_water",
                "sun_crops"
            }
        );
    }
    public static ProfessionVoiceLines cleric() {
        return new ProfessionVoiceLines(
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {
                "truthful",
                "redstone",
                "faith_protects",
                "trust_faith",
                "sinner",
                "good_heart",
                "pray_for_you",
                "deceiving_desires",
                "greed"
            },
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {}
        );
    }
    public static ProfessionVoiceLines armorer() {
        return new ProfessionVoiceLines(
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {
                "proud",
                "rusty",
                "want_smth",
                "work_hard",
                "best_armor",
                "sweaty"
            },
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {}
        );
    }
    public static ProfessionVoiceLines all() {
        return new ProfessionVoiceLines(
            new String[] {
                "reasonable_prices",
                "want_to_buy_smth",
                "buy_what_you_want",
                "good_stuff",
                "something_you_want_to_buy"
            },
            new String[] {
                "see_you",
                "nice_day",
                "nice_trading",
                "know_to_find_me"
            },
            new String[] {
                "yes",
                "very_good",
                "ok",
                "lets_do_it",
                "we_can_do_that",
                "sure"
            },
            new String[] {
                "thank_you",
                "saved_us",
                "you_are_a_hero",
                "hero"
            },
            new String[] {
                "hello",
                "sir",
                "hey",
                "good_morning",
                "good_to_see_you",
                "nice_day",
                "heya",
                "how_are_you",
                "pleasure",
                "things_going"
            },
            new String[] {
                "ahh",
                "haah",
                "breath",
                "humm",
                "whistle",
                "hmmm",
                "uff",
                "huh",
                "hugh",
                "head_scratching",
                "hmm_low_volume",
                "ahhh_low_volume"
            },
            new String[] {
                "more_clever",
                "feeling_better",
                "new_item",
                "friends",
                "special_offer",
                "enlightend"
            },
            new String[] {
                "wooh",
                "woohooo",
                "oh_yeah",
                "celeb_yes",
                "nice",
                "lets_go"
            },
            new String[] {
                "gratitude",
                "deserve_it"
            },
            new String[] {
                "i_am_sorry",
                "not_at_all",
                "nope",
                "cant_do_that",
                "no_sorry",
                "no",
                "oh_no"
            },
            new String[] {
                "gossip"
            },
            new String[] {
                "am_i_crazy",
                "not_crazy",
                "beach_house",
                "live_can_be_hard",
                "you_look_good",
                "eat_today",
                "i_like_you",
                "anything_i_want",
                "visit_me",
                "less_alone",
                "cant_people_be_nice",
                "utterly_useless",
                "dont_like_people",
                "underwear_itchy",
                "nice_guy",
                "violated",
                "nice_person",
                "dont_like_trees",
                "people_dont_like_me",
                "fence",
                "know_what_you_thinking",
                "you_look_funny",
                "you_awesome",
                "afraid_of_cats",
                "what_can_i_do_now"
            },
            new String[] {
                "death"
            },
            new String[] {
                "hurt"
            },
            new String[] {
                "wonderful_day",
                "beautiful_day",
                "sun_shining"
            },
            new String[] {
                "raining",
                "dry_standpoint",
                "cant_be_good",
                "much_rain"
            }
        );
    }

    public static ProfessionVoiceLines toolsmith() {
        return new ProfessionVoiceLines(
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {
                "tools_are_weapons",
                "sword",
                "lock_chest",
                "tools",
                "garden_project"
            },
            new String[] {},
            new String[] {},
            new String[] {},
            new String[] {}
        );
    }
    public static ProfessionVoiceLines cartograph() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "directions",
                        "dont_get_lost",
                        "compass"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines weaponsmith() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "dangerous_world",
                        "quarrel",
                        "throw_axe",
                        "sharp_sword",
                        "right_weapon"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines butcher() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "solution",
                        "do_it_for_you",
                        "sharp",
                        "fresh_meat"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines librarian() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "read_book",
                        "reading_refreshing",
                        "journey",
                        "story"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines fisherman() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "fish_net",
                        "boat",
                        "fresh_fish",
                        "straight_out_sea",
                        "relaxing",
                        "trash",
                        "nutrition"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines leatherworker() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "need_leather",
                        "nice_clothes",
                        "spend_leather_on_books",
                        "love_job",
                        "smell_bad",
                        "animals_leather"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines mason() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "need_build_wall",
                        "hammering_stones",
                        "value_stones",
                        "work_exhausting",
                        "stones_for_building",
                        "i_got_bricks"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines fletcher() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "buy_arrows",
                        "flint",
                        "need_arrows",
                        "long_bow"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines nitwit() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "important",
                        "bored",
                        "will_i_do",
                        "everybody_job",
                        "life_is_wonderful"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
    public static ProfessionVoiceLines shepherd() {
        return new ProfessionVoiceLines(
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {
                        "funny_sheep",
                        "wolves",
                        "need_to_shear",
                        "wool",
                        "banner"
                },
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {}
        );
    }
}
