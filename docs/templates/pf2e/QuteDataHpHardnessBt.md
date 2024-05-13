# QuteDataHpHardnessBt

Hit Points, Hardness, and a broken threshold for hazards and shields. Used for creatures, hazards, and shields.

Hazard example with a broken threshold and note:  <blockquote>**Hardness** 10, **HP (BT)** 30 (15) to destroy a channel gate</blockquote>

Hazard example with a name, broken threshold, and note:  <blockquote>**Floor Hardness** 10, **Floor HP** 30 (BT 15) to destroy a channel gate</blockquote>

Creature example with a name and ability:  <blockquote>**Head Hardness** 10, **Head HP** 30 (hydra regeneration)</blockquote>

## Attributes

[brokenThreshold](#brokenthreshold), [hardness](#hardness), [hp](#hp)


### hp

The HP as a [HpStat](QuteDataHpHardnessBt/HpStat.md) (optional)

### hardness

Hardness as a [SimpleStat](QuteDataGenericStat/SimpleStat.md) (optional)

### brokenThreshold

Broken threshold as an integer (optional, not populated for creatures)
