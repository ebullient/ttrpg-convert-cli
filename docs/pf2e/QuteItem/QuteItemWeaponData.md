# QuteItemWeaponData

Pf2eTools item weapon attributes

This data object provides a default mechanism for creating a marked up string based on the attributes that are present. To use it, reference it directly:  
 ```  
 {#for weapons in resource.weapons}  
 {weapons}  
 {/for}  
 ```  
 or, using `{#each}` instead:  
 ```  
 {#each resource.weapons}  
 {it}  
 {/each}  
 ```

## Attributes

[damage](#damage), [group](#group), [ranged](#ranged), [traits](#traits), [type](#type)


### damage


### group


### ranged


### traits

Formatted string. List of traits (links)

### type

Formatted string. Weapon type
