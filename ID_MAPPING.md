# Таблица ID: Thaumcraft 1.12.2 → порт 1.21.1

Совместимость со старыми мирами не нужна. Namespace везде `thaumcraft:`.

## Правила схемы

1. Только строчные буквы, `snake_case`, порядок слов как в ванилле: **[свойство/материал]_[предмет]**
   (`arcane_stone`, `thaumium_block`, `greatwood_planks`, `amber_ore`).
2. Если ID оригинала уже в таком виде — **оставляем как есть** (`amber_block`, `crucible`, `thaumometer`, `salis_mundus`).
3. Варианты по metadata → отдельные ID. Имя строится по английскому названию варианта из `en_us.lang`.
4. Окончания брони и инструментов — ванильные: `_helmet`, `_chestplate`, `_leggings`, `_boots`, `_pickaxe`.
   Для нестандартных частей брони (robe, hood, cuirass, greaves…) — по английскому названию.
5. Цвета — ванильные имена 1.21: `lightblue` → `light_blue`, `silver` → `light_gray`.
6. Двойные плиты (`slab_double_*`) исчезают: в 1.21 плита — один блок со свойством `type=double`.
7. Технические блоки (`placeholder_*`, `effect_*`, `empty`, `hole`, `barrier`) и жидкости — имена оригинала.
8. Ключи перевода следуют новым ID (`block.thaumcraft.<id>`, `item.thaumcraft.<id>`), значения — из старого ключа.
9. Таблица дополняется на каждом этапе (сущности, эффекты, звуки и т.п. — по мере переноса).

## Предметы с вариантами (metadata)

| Старый ID | meta | Вариант | Название (en) | Новый ID |
|---|---|---|---|---|
| ingot | 0 | thaumium | Thaumium Ingot | `thaumium_ingot` |
| ingot | 1 | void | Void Metal Ingot | `void_metal_ingot` |
| ingot | 2 | brass | Alchemical Brass Ingot | `brass_ingot` |
| nugget | 0 | iron | Iron Nugget | `iron_nugget` |
| nugget | 1 | copper | Copper Nugget | `copper_nugget` |
| nugget | 2 | tin | Tin Nugget | `tin_nugget` |
| nugget | 3 | silver | Silver Nugget | `silver_nugget` |
| nugget | 4 | lead | Lead Nugget | `lead_nugget` |
| nugget | 5 | quicksilver | Quicksilver Drop | `quicksilver_drop` |
| nugget | 6 | thaumium | Thaumium Nugget | `thaumium_nugget` |
| nugget | 7 | void | Void Metal Nugget | `void_metal_nugget` |
| nugget | 8 | brass | Alchemical Brass Nugget | `brass_nugget` |
| nugget | 9 | quartz | Quartz Sliver | `quartz_sliver` |
| nugget | 10 | rareearth | Rare Earths | `rare_earths` |
| cluster | 0 | iron | Native Iron Cluster | `iron_cluster` |
| cluster | 1 | gold | Native Gold Cluster | `gold_cluster` |
| cluster | 2 | copper | Native Copper Cluster | `copper_cluster` |
| cluster | 3 | tin | Native Tin Cluster | `tin_cluster` |
| cluster | 4 | silver | Native Silver Cluster | `silver_cluster` |
| cluster | 5 | lead | Native Lead Cluster | `lead_cluster` |
| cluster | 6 | cinnabar | Native Cinnabar Cluster | `cinnabar_cluster` |
| cluster | 7 | quartz | Native Quartz Cluster | `quartz_cluster` |
| plate | 0 | brass | Brass Plate | `brass_plate` |
| plate | 1 | iron | Iron Plate | `iron_plate` |
| plate | 2 | thaumium | Thaumium Plate | `thaumium_plate` |
| plate | 3 | void | Void Metal Plate | `void_metal_plate` |
| mind | 0 | clockwork | Clockwork Mind | `clockwork_mind` |
| mind | 1 | biothaumic | Biothaumic Mind | `biothaumic_mind` |
| module | 0 | vision | Vision Module | `vision_module` |
| module | 1 | aggression | Aggression Module | `aggression_module` |
| chunk | 0 | beef | Beef Nugget | `beef_chunk` |
| chunk | 1 | chicken | Chicken Nugget | `chicken_chunk` |
| chunk | 2 | pork | Pork Nugget | `pork_chunk` |
| chunk | 3 | fish | Fish Nugget | `fish_chunk` |
| chunk | 4 | rabbit | Rabbit Nugget | `rabbit_chunk` |
| chunk | 5 | mutton | Mutton Nugget | `mutton_chunk` |
| phial | 0 | empty | Glass Phial | `phial` |
| phial | 1 | filled | Phial of Essentia | `essentia_phial` |
| label | 0 | blank | Blank Label | `label` |
| label | 1 | filled | Marked Label | `marked_label` |
| baubles | 0 | amulet_mundane | Mundane Amulet | `mundane_amulet` |
| baubles | 1 | ring_mundane | Mundane Ring | `mundane_ring` |
| baubles | 2 | girdle_mundane | Mundane Belt | `mundane_belt` |
| baubles | 3 | ring_apprentice | Apprentice's Ring | `apprentice_ring` |
| baubles | 4 | amulet_fancy | Fancy Amulet | `fancy_amulet` |
| baubles | 5 | ring_fancy | Fancy Ring | `fancy_ring` |
| baubles | 6 | girdle_fancy | Fancy Belt | `fancy_belt` |
| amulet_vis | 0 | found | Vis Stone | `vis_stone` |
| amulet_vis | 1 | crafted | Amulet of Vis | `vis_amulet` |
| curio | 0 | arcane | Arcane Curiosity | `arcane_curio` |
| curio | 1 | preserved | Preserved Curiosity | `preserved_curio` |
| curio | 2 | ancient | Ancient Curiosity | `ancient_curio` |
| curio | 3 | eldritch | Eldritch Curiosity | `eldritch_curio` |
| curio | 4 | knowledge | Illuminating Curiosity | `illuminating_curio` |
| curio | 5 | twisted | Twisted Curiosity | `twisted_curio` |
| curio | 6 | rites | Crimson Rites | `crimson_rites` |
| loot_bag | 0 | common | Common Treasure | `common_loot_bag` |
| loot_bag | 1 | uncommon | Uncommon Treasure | `uncommon_loot_bag` |
| loot_bag | 2 | rare | Rare Treasure | `rare_loot_bag` |
| thaumonomicon | 0 | normal | Thaumonomicon | `thaumonomicon` |
| thaumonomicon | 1 | cheat | Cheater's Thaumonomicon | `cheaters_thaumonomicon` |
| celestial_notes | 0 | sun | Solar | `celestial_notes_sun` |
| celestial_notes | 1–4 | stars_1..4 | Stellar, N/S/W/E Quadrant | `celestial_notes_stars_1` … `_4` |
| celestial_notes | 5–12 | moon_1..8 | Lunar, … | `celestial_notes_moon_1` … `_8` |
| turret | 0 | basic | Automated Crossbow | `crossbow_turret` |
| turret | 1 | advanced | Advanced Automated Crossbow | `advanced_crossbow_turret` |
| turret | 2 | bore | Arcane Bore | `arcane_bore` |
| seal | 0 | blank | Blank Seal | `blank_seal` |
| seal | — | pickup / pickup_advanced | Control Seal: Collect | `collect_seal` / `advanced_collect_seal` |
| seal | — | fill / fill_advanced | Control Seal: Store | `store_seal` / `advanced_store_seal` |
| seal | — | empty / empty_advanced | Control Seal: Empty | `empty_seal` / `advanced_empty_seal` |
| seal | — | guard / guard_advanced | Control Seal: Guard | `guard_seal` / `advanced_guard_seal` |
| seal | — | harvest | Control Seal: Harvest | `harvest_seal` |
| seal | — | butcher | Control Seal: Butcher | `butcher_seal` |
| seal | — | lumber | Control Seal: Lumberjack | `lumberjack_seal` |
| seal | — | breaker | Control Seal: Block Breaker | `breaker_seal` |
| seal | — | use | Control Seal: Use | `use_seal` |
| seal | — | provider | Control Seal: Provide | `provide_seal` |
| seal | — | stock | Control Seal: Stock | `stock_seal` |
| creative_placer | 0–2 | obelisk / node / caster | (творческие) | `obelisk_placer` / `node_placer` / `caster_placer` |

Не варианты (состояние хранится в Data Components, ID один): `crystal_essence` (аспект), `primordial_pearl`
(прочность mote/nodule/pearl), `verdant_charm` (тип), `caster_basic`, фокусы `focus_1..3` — уточняется на этапах 4, 12, 13.

## Блоки (переименование под ванильный стиль)

| Старый ID | Название (en) | Новый ID |
|---|---|---|
| stone_arcane | Arcane Stone | `arcane_stone` |
| stone_arcane_brick | Arcane Stone Brick | `arcane_stone_bricks` |
| stone_ancient | Ancient Stone | `ancient_stone` |
| stone_ancient_tile | Ancient Stone Tile | `ancient_stone_tile` |
| stone_ancient_rock | Ancient Rock | `ancient_rock` |
| stone_ancient_glyphed | Glyphed Stone | `glyphed_stone` |
| stone_ancient_doorway | Ancient Barrier | `ancient_barrier` |
| stone_eldritch_tile | Eldritch Stone | `eldritch_stone` |
| stone_porous | Porous Stone | `porous_stone` |
| matrix_speed / matrix_cost | Infusion Speed/Cost Stone | `infusion_speed_stone` / `infusion_cost_stone` |
| metal_thaumium / metal_void / metal_brass | Thaumium/Void Metal/Brass Block | `thaumium_block` / `void_metal_block` / `brass_block` |
| metal_alchemical / _advanced | (Advanced) Alchemical Construct | `alchemical_construct` / `advanced_alchemical_construct` |
| ore_amber / ore_cinnabar / ore_quartz | Amber Bearing Stone / Cinnabar Ore / Quartz Ore | `amber_ore` / `cinnabar_ore` / `quartz_ore` |
| amber_brick | Amber Bricks | `amber_bricks` |
| log_greatwood / log_silverwood | Greatwood/Silverwood Log | `greatwood_log` / `silverwood_log` |
| leaves_greatwood / leaves_silverwood | … Leaves | `greatwood_leaves` / `silverwood_leaves` |
| sapling_greatwood / sapling_silverwood | … Sapling | `greatwood_sapling` / `silverwood_sapling` |
| plank_greatwood / plank_silverwood | … Planks | `greatwood_planks` / `silverwood_planks` |
| stairs_greatwood / stairs_silverwood | … Stairs | `greatwood_stairs` / `silverwood_stairs` |
| stairs_arcane / stairs_arcane_brick / stairs_ancient | … Stairs | `arcane_stone_stairs` / `arcane_brick_stairs` / `ancient_stone_stairs` |
| slab_greatwood (+double) | Greatwood Slab | `greatwood_slab` |
| slab_silverwood (+double) | Silverwood Slab | `silverwood_slab` |
| slab_arcane_stone (+double) | Arcane Stone Slab | `arcane_stone_slab` |
| slab_arcane_brick (+double) | Arcane Brick Slab | `arcane_brick_slab` |
| slab_ancient (+double) | Ancient Stone Slab | `ancient_stone_slab` |
| slab_eldritch (+double) | Eldritch Stone Slab | `eldritch_stone_slab` |
| crystal_aer … crystal_vitium | Air … Flux Crystal | `aer_crystal`, `aqua_crystal`, `ignis_crystal`, `terra_crystal`, `ordo_crystal`, `perditio_crystal`, `vitium_crystal` |
| pedestal_arcane / _ancient / _eldritch | … Pedestal | `arcane_pedestal` / `ancient_pedestal` / `eldritch_pedestal` |
| pillar_arcane / _ancient / _eldritch | (…) Infusion Pillar | `infusion_pillar` / `ancient_infusion_pillar` / `eldritch_infusion_pillar` |
| table_wood / table_stone | Wood/Stone Table | `wooden_table` / `stone_table` |
| jar_normal / jar_void / jar_brain | Warded Jar / Void Jar / Brain in a Jar | `warded_jar` / `void_jar` / `brain_jar` |
| tube / tube_valve / tube_filter / tube_restrict / tube_oneway / tube_buffer | Essentia Tube … Buffer | `essentia_tube` / `essentia_valve` / `filtered_essentia_tube` / `restricted_essentia_tube` / `directional_essentia_tube` / `essentia_buffer` |
| smelter_basic / _thaumium / _void | Essentia Smeltery … | `essentia_smeltery` / `thaumium_essentia_smeltery` / `void_metal_essentia_smeltery` |
| smelter_aux / smelter_vent | Auxiliary Slurry Pump / Venting Port | `auxiliary_slurry_pump` / `auxiliary_venting_port` |
| lamp_arcane / lamp_growth / lamp_fertility | Arcane Lamp / Lamp of Growth / of Fertility | `arcane_lamp` / `growth_lamp` / `fertility_lamp` |
| mirror / mirror_essentia | Magic/Essentia Mirror | `magic_mirror` / `essentia_mirror` |
| loot_crate_* / loot_urn_* | Common/Uncommon/Rare Crate/Urn | `common_crate`, `uncommon_crate`, `rare_crate`, `common_urn`, `uncommon_urn`, `rare_urn` |
| paving_stone_travel / paving_stone_barrier | Paving Stone of Travel / Barrier Stone | `travel_paving_stone` / `barrier_paving_stone` |
| wand_workbench | Focal Manipulator | `focal_manipulator` |
| golem_builder | Golem Press | `golem_press` |
| infusion_matrix | Runic Matrix | `infusion_matrix` (оставлен) |
| brain_box | Mnemonic Matrix | `mnemonic_matrix` |
| activator_rail | Arcane Activator Rail | `arcane_activator_rail` |
| essentia_input / essentia_output | Filling/Emptying Essentia Transfuser | `essentia_input` / `essentia_output` (оставлены) |
| condenser_lattice_dirty | Clogged Flux Condenser Lattice | `clogged_condenser_lattice` |
| taint_log | Taintwood Log | `taintwood_log` |
| nitor_&lt;цвет&gt; | … Nitor | `<цвет>_nitor` (`white_nitor`, `light_gray_nitor`, `light_blue_nitor` …) |
| candle_&lt;цвет&gt; | … Tallow Candle | `<цвет>_candle` |
| banner_&lt;цвет&gt; / banner_crimson_cult | … Banner | `<цвет>_banner` / `crimson_cult_banner` |

Без изменений: `amber_block`, `arcane_workbench`, `arcane_workbench_charger`, `crucible`, `research_table`, `alembic`, `centrifuge`,
`bellows`, `arcane_ear`, `arcane_ear_toggle`, `levitator`, `dioptra`, `hungry_chest`, `infernal_furnace`, `spa`, `potion_sprayer`,
`recharge_pedestal`, `redstone_relay`, `pattern_crafter`, `stabilizer`, `vis_battery`, `vis_generator`, `condenser`, `condenser_lattice`,
`thaumatorium`, `thaumatorium_top`, `everfull_urn`, `flesh_block`, `grass_ambient`, `shimmerleaf`, `cinderpearl`, `vishroom`,
`taint_soil`, `taint_rock`, `taint_crust`, `taint_geyser`, `taint_fibre`, `taint_feature`, `flux_goo`, `liquid_death`, `purifying_fluid`,
`placeholder_*`, `effect_*`, `empty`, `hole`, `barrier`.

## Предметы без вариантов (переименование)

| Старый ID | Новый ID |
|---|---|
| thaumium_pick / void_pick / elemental_pick | `thaumium_pickaxe` / `void_metal_pickaxe` / `elemental_pickaxe` |
| void_sword / void_axe / void_shovel / void_hoe | `void_metal_sword` / `void_metal_axe` / `void_metal_shovel` / `void_metal_hoe` |
| thaumium_helm / _chest / _legs / _boots | `thaumium_helmet` / `thaumium_chestplate` / `thaumium_leggings` / `thaumium_boots` |
| void_helm / _chest / _legs / _boots | `void_metal_helmet` / `void_metal_chestplate` / `void_metal_leggings` / `void_metal_boots` |
| cloth_chest / cloth_legs / cloth_boots | `thaumaturge_robe` / `thaumaturge_leggings` / `thaumaturge_boots` |
| void_robe_helm / _chest / _legs | `void_thaumaturge_hood` / `void_thaumaturge_robe` / `void_thaumaturge_leggings` |
| fortress_helm / _chest / _legs | `fortress_helmet` / `fortress_cuirass` / `fortress_thigh_guards` |
| crimson_plate_helm / _chest / _legs | `crimson_cult_helmet` / `crimson_cult_chestplate` / `crimson_cult_greaves` |
| crimson_robe_helm / _chest / _legs | `crimson_cult_hood` / `crimson_cult_robe` / `crimson_cult_leggings` |
| crimson_praetor_helm / _chest / _legs | `crimson_praetor_helmet` / `crimson_praetor_chestplate` / `crimson_praetor_greaves` |
| crimson_boots | `crimson_cult_boots` |
| traveller_boots | `traveller_boots` (оставлен) |
| bucket_death / bucket_pure | `liquid_death_bucket` / `purifying_fluid_bucket` |
| brain | `zombie_brain` |
| bottle_taint | `bottle_of_taint` |
| resonator | `essentia_resonator` |
| filter | `essentia_filter` |
| jar_brace | `jar_brace` (оставлен) |
| mechanism_simple / mechanism_complex | `simple_arcane_mechanism` / `complex_arcane_mechanism` |
| fabric | `enchanted_fabric` |
| tallow | `magic_tallow` |
| grapple_gun / _tip / _spool | `grappler` / `grappler_head` / `grappler_spool` |
| hand_mirror | `hand_mirror` (оставлен) |
| golem_bell | `golem_bell` (оставлен) |

Остальные предметы (`alumentum`, `amber`, `quicksilver`, `salis_mundus`, `thaumometer`, `scribing_tools`, `goggles`, `sanity_checker`,
`sanity_soap`, `bath_salts`, `void_seed`, `mirrored_glass`, `morphic_resonator`, `vis_resonator`, `triple_meat_treat`, `causality_collapser`,
`primal_crusher`, `crimson_blade`, `pech_wand`, `focus_pouch`, `charm_undying`, `cloud_ring`, `curiosity_band`, `voidseer_charm`,
`creative_flux_sponge`, `enchanted_placeholder`, `thaumium_sword/axe/shovel/hoe`, `elemental_sword/axe/shovel/hoe`) — без изменений.

## Ресурсы с заглавными буквами

| Было | Стало |
|---|---|
| `textures/misc/particleMote.png` | `textures/misc/particle_mote.png` |
| `textures/models/Thaumaturge_eyes.png` | `textures/models/thaumaturge_eyes.png` |
