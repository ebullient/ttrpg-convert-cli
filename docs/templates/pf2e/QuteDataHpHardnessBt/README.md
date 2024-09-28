# QuteDataHpHardnessBt

Hit Points, Hardness, and a broken threshold for hazards and shields. Used for creatures, hazards, and shields.

Hazard example with a broken threshold and note:

```md
**Hardness** 10, **HP (BT)** 30 (15) to destroy a channel gate
```

Hazard example with a name, broken threshold, and note:

```md
**Floor Hardness** 10, **Floor HP** 30 (BT 15) to destroy a channel gate
```

Creature example with a name and ability:

```md
**Head Hardness** 10, **Head HP** 30 (hydra regeneration)
```

## Attributes

[brokenThreshold](#brokenthreshold), [hardness](#hardness), [hp](#hp)


### brokenThreshold

Broken threshold as an integer (optional, not populated for creatures)

### hardness

Hardness as a [SimpleStat](../QuteDataGenericStat/SimpleStat.md)
(optional)

### hp

The HP as a [HpStat](HpStat.md) (optional)
