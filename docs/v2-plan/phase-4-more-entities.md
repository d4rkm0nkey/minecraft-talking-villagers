# Phase 4 – Witch, Wandering Trader und Illager

## Status: erledigt

Umgesetzt auf Branch `v2/phase-4`. Getestet auf Dedicated Servern mit Fabric und NeoForge über RCON:
- Witch, Wandering Trader, Pillager, Vindicator, Evoker und Illusioner bekommen Stimmen aus einem Test-Pack (die Illager-Stimme nutzt `entities: ["illager"]`). Andere Mobs bekommen keine.
- Witches sind standardmäßig weiblich.
- `summon_vex` ohne eigene Zeilen fällt auf `cast_spell` zurück.

**Offen:** Ereignisse, die nur mit KI und echtem Spieler auftreten (Spieler entdecken, Angriff, Armbrust spannen, Raid-Horn, Trank trinken und werfen, Lachen, Handel mit dem Trader), und das Hören im Client.

**Client-Test (10.10.2026):** Im echten Client bestätigt: Villager, Hexe, Händler und Illager sprechen mit den Stimmen aus den Test-Packs, die Vanilla-Laute werden ersetzt.

Umsetzung und Abweichungen:
- **Ein Sound-Hook für alles:** Der zentrale Mixin sitzt jetzt auf `Entity.playSound(SoundEvent, float, float)` statt auf `makeSound`. Damit sind auch die Zauber-Sounds der Spellcaster abgedeckt. Die Tabelle `VanillaSounds` ordnet jedem Vanilla-Sound pro Entity-Art ein Event zu. Das „Wololo“ des Evokers bleibt bewusst Vanilla.
- **Gezielte Hooks** gibt es nur, wo Vanilla am Entity vorbei abspielt oder kein Sound existiert: Witch-Trank (`Level.playSound`), Spieler entdecken (`Mob.setTarget`), Nahkampf (`Mob.doHurtTarget`), Fernkampf (Pillager, Illusioner), Armbrust spannen, Raid-Horn (`Raid.playSound`), Lachen beim Töten (`LivingEntity.die`).
- **Handel** hängt jetzt an `AbstractVillager` (`setTradingPlayer`, `notifyTrade`) und gilt damit für Villager und Trader gleichermaßen.
- **`disappear`** ist der Moment, in dem der Trader seinen Unsichtbarkeitstrank getrunken hat (Vanilla-Sound `wandering_trader.disappeared`). Beim echten Despawn verschwindet die Entity samt Ton sofort, eine Zeile dort wäre nie zu hören.
- **`Chatter`** (vorher `VillagerChatter`) gilt auch für den Trader, der aber nur grüßt und keinen Smalltalk macht.
- **Sprecher-Limit:** Höchstens 3 Entities sprechen gleichzeitig im Umkreis von 32 Blöcken. Hurt, Death und andere unterbrechende Events zählen nicht mit.
- **Events prüfen ihre Entity-Art:** Ein Event wird nur ausgelöst, wenn es laut Katalog zur Entity-Art passt.

## Ziel
Stimmen für Witch, Wandering Trader, Pillager, Vindicator, Evoker und Illusioner, gebaut auf derselben Infrastruktur wie in Phase 3. Ein neuer Entity-Typ bedeutet nur noch: ein `EntityKind`, eine `EventPolicy`-Tabelle und ein paar Hook-Mixins.

## Generalisierung aus Phase 3
- [ ] `VoiceController` ist generisch über `EntityKind`. Villager-spezifische Teile (Beruf, Gossip, Hero) stecken in einem `VillagerBehavior`. Alle anderen bekommen ein eigenes `EntityVoiceBehavior`.
- [ ] Gemeinsame Hooks für **alle** Mob-Typen über einen Mixin auf `Mob`/`LivingEntity`: `getAmbientSound`, `getHurtSound`, `getDeathSound`. Der Controller prüft, ob der Typ unterstützt wird. Ein Mixin ersetzt so sechs.
- [ ] Mapping `EntityType → EntityKind` als Registry, damit es später für weitere Mobs erweiterbar ist (Piglin? Siehe Backlog).

## Entity-Details

### Witch
| Event | Hook |
|---|---|
| `ambient`, `hurt`, `death` | gemeinsam |
| `drink_potion` | `Witch.aiStep`, wenn ein Trank zum Trinken gesetzt wird (`setUsingItem`) |
| `throw_potion` | `performRangedAttack` |
| `laugh` | Ziel stirbt in der Nähe bzw. Trank trifft (Kill-Event) |
| `celebrate` | Raid gewonnen (`Raider.celebrate`) |

Default-Geschlecht für Witches: Config `witchGender` = `female` (Default) / `random`, weil Vanilla-Witches als weiblich gelten. Für die Auswahl gilt `gender` wie gewohnt.

### Wandering Trader
| Event | Hook |
|---|---|
| `ambient`, `hurt`, `death` | gemeinsam |
| `greeting` | Spieler in Nähe, gleiche Logik wie beim Villager (geteilte `GreetingPolicy`) |
| `trade`, `trade_success`, `yes`, `no` | Merchant-Hooks. `AbstractVillager` ist die gemeinsame Basis mit Villager, die Hooks werden geteilt. |
| `drink_potion` | Unsichtbarkeitstrank / Milch (`UseItemGoal`) |
| `disappear` | kurz vor dem Despawn (`despawnDelay` erreicht 0) |

Kinderstimmen: nicht relevant, Trader sind nie Babys.

### Illager (Pillager, Vindicator, Evoker, Illusioner)
Gemeinsame Basis `AbstractIllager`/`Raider`:

| Event | Hook | Gilt für |
|---|---|---|
| `ambient`, `hurt`, `death` | gemeinsam | alle |
| `spot_player` | Ziel wird erstmals gesetzt (`setTarget` von null auf Spieler) | alle |
| `attack` | Nahkampftreffer / Schuss | alle |
| `charge` | Armbrust spannen | Pillager |
| `celebrate` | `Raider.setCelebrating(true)` | alle |
| `raid_horn` | Raid-Welle startet (Raid-Captain) | Captain |
| `cast_spell` | `SpellcasterIllager` beginnt zu zaubern | Evoker, Illusioner |
| `summon_vex`, `fangs` | spezifischer Spell | Evoker |
| `mirror` | Spiegelbild-Spell | Illusioner |

- `entities` im Manifest erlaubt die Werte `pillager`, `vindicator`, `evoker`, `illusioner` sowie den Sammelbegriff `illager`, der auf alle vier expandiert.
- Ist für ein Event keine Zeile vorhanden, greift das Event der Elterngruppe. Beispiel: `summon_vex` fällt auf `cast_spell` zurück, und das fällt auf Vanilla zurück. Die Fallback-Kette steht im Event-Katalog (`VoiceEvent.parent`).
- **Spam-Schutz:** Bei Raids sind 20+ Illager gleichzeitig aktiv. Deshalb gibt es ein globales Budget pro Gebiet: maximal N gleichzeitige Sprecher im Umkreis von 32 Blöcken, per Config, Default 3. Umgesetzt wird das im `SpeechTracker` mit einem Chunk-basierten Zähler.

### Gemeinsame Regeln
- Geschlecht und Sprache funktionieren wie beim Villager (Sprache für Illager: Phase 5).
- Pro Entity-Typ in der Config ein- und ausschaltbar.

## Tests
- GameTest pro Entity: Spawn → Stimme zugewiesen → `hurt` löst ein Payload aus.
- Raid-Szenario: Das Sprecher-Budget wird eingehalten.

## Ergebnis
Alle sechs zusätzlichen Entity-Typen sprechen, falls ein Pack Stimmen dafür hat, und fallen sauber auf Vanilla zurück, falls nicht.
