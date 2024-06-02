# QuteDataDefenses

Pf2eTools Armor class, Saving Throws, and other attributes describing defenses of a creature or hazard. Example:

- **AC** 23 (33 with mage armor); **Fort** +15, **Ref** +12, **Will** +10
- **Floor Hardness** 18, **Floor HP** 72 (BT 36); **Channel Hardness** 12, **Channel HP** 48 (BT24 ) to destroy a channel gate; **Immunities** critical hits; **Resistances** precision damage; **Weaknesses** bludgeoning damage

## Attributes

[ac](#ac), [additionalHpHardnessBt](#additionalhphardnessbt), [hpHardnessBt](#hphardnessbt), [immunities](#immunities), [resistances](#resistances), [savingThrows](#savingthrows), [weaknesses](#weaknesses)


### ac

The armor class as a [QuteDataArmorClass](QuteDataArmorClass.md)

### savingThrows

The saving throws, as [QuteSavingThrows](QuteDataDefenses/QuteSavingThrows.md)

### hpHardnessBt

HP, hardness, and broken threshold stored in a [QuteDataHpHardnessBt](QuteDataHpHardnessBt.md)

### additionalHpHardnessBt

Additional HP, hardness, or broken thresholds for other HP components as a map of names to [QuteDataHpHardnessBt](QuteDataHpHardnessBt.md)

### immunities

List of strings, optional

### resistances

Map of (name, [QuteDataGenericStat](QuteDataGenericStat.md))

### weaknesses

Map of (name, [QuteDataGenericStat](QuteDataGenericStat.md))
