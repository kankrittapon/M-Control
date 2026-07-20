# World Core Compatibility API

Unknown modded damage remains Vanilla by default. The compatibility API changes protection metadata;
it never opts damage into AP, Accuracy, Critical, Special Attack, or RPG DR automatically.

## Impact Classification

Listen to `CombatImpactResolveEvent` on `MinecraftForge.EVENT_BUS`:

```java
@SubscribeEvent
public static void classifyImpact(CombatImpactResolveEvent event) {
    if (event.source() != null && isMyExplosion(event.source())) {
        event.setCategory(CombatImpactCategory.EXPLOSION);
        event.setOrigin(findExplosionCenter(event.source()));
    }
}
```

Handlers may override `category`, `origin`, and the `rpgTagged` metadata flag. An origin must contain
finite coordinates. Setting `rpgTagged=true` only identifies the impact to protection/debug systems.
To use BRPG damage math, call `RpgCombatService.apply(RpgDamageContext)` directly.

The same event receives knockback-only impacts with `source=null` and category `UNKNOWN`. A mod that
owns the impulse may provide its known origin/category. Otherwise the resolver safely uses the Forge
knockback vector and does not invent an attacker.

## Enchantment Policy

Static registration by stable ID:

```java
RpgEnchantmentPolicyRegistry.register(
    new ResourceLocation("example", "impact_power"),
    EnchantmentCombatPolicy.RPG_BRIDGED);
```

Dynamic override:

```java
@SubscribeEvent
public static void enchantPolicy(EnchantmentCombatPolicyEvent event) {
    if (event.enchantmentId().equals(MY_ENCHANTMENT_ID)) {
        event.setPolicy(EnchantmentCombatPolicy.RPG_BRIDGED);
    }
}
```

Policies:

- `VANILLA_ONLY`: unchanged outside RPG attack execution and never copied into RPG skill math.
- `RPG_BRIDGED`: reserved for an explicit adapter that translates the enchantment into an RPG payload.
- `DISABLED_IN_RPG_COMBAT`: excluded while weapons are drawn.

Registering `RPG_BRIDGED` does not itself add damage or knockback. The integrating mod must provide a
reviewed adapter so one enchantment cannot apply through both Vanilla and RPG paths.
