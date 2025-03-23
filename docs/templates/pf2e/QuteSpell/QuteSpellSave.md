# QuteSpellSave

Details about the saving throw for a spell.

Example default representations:

- `basic Reflex or Fortitude`
- `basic Reflex, Fortitude, or Willpower`

## Attributes

[basic](#basic), [hidden](#hidden), [saves](#saves)

### basic

True if this is a basic save (boolean)

### hidden

Whether this save should be hidden. This is sometimes true when it's a special save that is
described in the text of the spell.

### saves

The saving throws that can be used for this spell (list of strings)
