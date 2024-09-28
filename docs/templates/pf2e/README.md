# Pf2eTools templates

Qute templates for generating content from Pf2eTools data.

Pathfinder data uses a lot of inline and nested embedding,
which creates additional template variants and some special
behavior.

## References

- [Pf2eQuteBase](Pf2eQuteBase.md): Attributes for notes that are generated from the Pf2eTools data.
- [Pf2eQuteNote](Pf2eQuteNote.md): Attributes for notes that are generated from the Pf2eTools data.
- [QuteAbility](QuteAbility.md): Pf2eTools Ability attributes (`ability2md.txt` or `inline-ability2md.txt`).
- [QuteAction](QuteAction/README.md): Pf2eTools Action attributes (`action2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteAffliction](QuteAffliction/README.md): Pf2eTools Affliction attributes (inline/embedded, `inline-affliction2md.txt`)

Extension of [Pf2eQuteNote](Pf2eQuteNote.md)
- [QuteArchetype](QuteArchetype.md): Pf2eTools Archetype attributes (`archetype2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteBackground](QuteBackground.md): Pf2eTools Background attributes (`background2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteBook](QuteBook/README.md): Pf2eTools Book attributes (`book2md.txt`)

Extension of [Pf2eQuteNote](Pf2eQuteNote.md)
- [QuteCreature](QuteCreature/README.md): Pf2eTools Creature attributes (`creature2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteDataActivity](QuteDataActivity.md): Pf2eTools activity attributes.
- [QuteDataArmorClass](QuteDataArmorClass.md): Pf2eTools armor class attributes.
- [QuteDataDefenses](QuteDataDefenses/README.md): Pf2eTools Armor class, Saving Throws, and other attributes describing defenses of a creature or hazard.
- [QuteDataFrequency](QuteDataFrequency.md): A description of a frequency e.g.
- [QuteDataHpHardnessBt](QuteDataHpHardnessBt/README.md): Hit Points, Hardness, and a broken threshold for hazards and shields.
- [QuteDataRange](QuteDataRange/README.md): A range with a given value and unit of measurement for that value.
- [QuteDataSpeed](QuteDataSpeed.md): Examples:

- `10 feet, swim 20 feet (some note); some ability`
- `10 feet, swim 20 feet, some ability`
- [QuteDataTimedDuration](QuteDataTimedDuration/README.md): A duration of time, represented by a numerical value and a unit.
- [QuteDeity](QuteDeity/README.md): Pf2eTools Deity attributes (`deity2md.txt`)

Deities are rendered both standalone and inline (as an admonition block).
- [QuteFeat](QuteFeat.md): Pf2eTools Feat attributes (`feat2md.txt`)

Feats are rendered both standalone and inline (as an admonition block).
- [QuteHazard](QuteHazard/README.md): Pf2eTools Hazard attributes (`hazard2md.txt`)

Hazards are rendered both standalone and inline (as an admonition block).
- [QuteInlineAttack](QuteInlineAttack/README.md): Pf2eTools Attack attributes (inline/embedded, `inline-attack2md.txt`)

When used directly, renders according to `inline-attack2md.txt`
- [QuteItem](QuteItem/README.md): Pf2eTools Item attributes

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteRitual](QuteRitual/README.md): Pf2eTools Ritual attributes (`ritual2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteSpell](QuteSpell/README.md): Pf2eTools Spell attributes (`spell2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteTrait](QuteTrait.md): Pf2eTools Trait attributes (`trait2md.txt`)

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)
- [QuteTraitIndex](QuteTraitIndex.md): Pf2eTools Trait index attributes (`indexTrait.md`)

This replaces the index usually generated for folders.
