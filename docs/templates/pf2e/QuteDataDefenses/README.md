# QuteDataDefenses

Pf2eTools Armor class, Saving Throws, and other attributes describing defenses of a creature or hazard.

Example:

```md
**AC** 23 (33 with mage armor); **Fort** +15, **Ref** +12, **Will** +10
```

```md
**Floor Hardness** 18, **Floor HP** 72 (BT 36);
**Channel Hardness** 12, **Channel HP** 48 (BT24 ) to destroy a channel gate;
**Immunities** critical hits;
**Resistances** precision damage;
**Weaknesses** bludgeoning damage
```

## Attributes

[ac](#ac), [additionalHpHardnessBt](#additionalhphardnessbt), [hpHardnessBt](#hphardnessbt), [immunities](#immunities), [resistances](#resistances), [savingThrows](#savingthrows), [weaknesses](#weaknesses)


### ac

The armor class as a [QuteDataArmorClass](../QuteDataArmorClass.md)

### additionalHpHardnessBt

Additional HP, hardness, or broken thresholds for other HP components as a map of
names to [QuteDataHpHardnessBt](../QuteDataHpHardnessBt/README.md)

### hpHardnessBt

HP, hardness, and broken threshold stored in a [QuteDataHpHardnessBt](../QuteDataHpHardnessBt/README.md)

### immunities

List of strings, optional

### resistances

Map of (name, [QuteDataGenericStat](../QuteDataGenericStat/README.md))

### savingThrows

The saving throws, as [QuteSavingThrows](QuteSavingThrows.md)

### weaknesses

Map of (name, [QuteDataGenericStat](../QuteDataGenericStat/README.md))
