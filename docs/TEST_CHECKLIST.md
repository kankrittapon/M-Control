# BRPG Test Checklist

อัปเดต: 2026-07-20

สถานะ:

- `[x]` ผ่านและมีหลักฐาน
- `[~]` เคยผ่านบางส่วน แต่ต้อง Retest หลังการเปลี่ยนล่าสุด
- `[ ]` ยังไม่ได้ทดสอบด้วยผู้เล่น
- `[A]` Automated test ครอบคลุมแล้ว แต่ยังไม่แทนการตรวจ feel ใน Client

## 1. Automated Baseline

- [x] `gradlew test build` ผ่านล่าสุด 2026-07-21 (`92` unit tests)
- [x] `gradlew runGameTestServer` ผ่านล่าสุด 2026-07-20 (`18/18` required GameTests)
- [x] Vanilla Combat Bridge Phase F deterministic probes ผ่าน front/side/rear สำหรับ Perfect Guard
- [x] Live Zoglin/Zombie front melee, Skeleton rear projectile และ Creeper rear explosion ตรงตามกฎ
- [A] Spellbound Heart I-V definitions: 30s cooldown, 5/6/7/8/10m duration, MP pulse every 200 ticks,
  recovery 50/100/150/200/250 and Movement Speed +10%
- [ ] Client: Spellbound Heart Rank V ฟื้น 250 MP ที่ 10 วินาทีโดยไม่ใช้ MP ตอนร่าย และ speed กลับปกติเมื่อหมด
- [A] Speed Spell I-III: MP `120/130/140`, cooldown `90/75/50s`, speed `10/15/20%` for `30s`
- [ ] Client: Speed Spell III applies to self, expires after 30s, and restores all three attributes
- [ ] Multiplayer: Speed Spell affects at most 10 nearby allies and excludes players outside radius 15
- [A] Sage's Memory: Level 20, SP 10, cooldown 4200 ticks, instant-cast state 300 ticks
- [ ] Client: Sage's Memory removes only pre-hit wind-up and expires after 15 seconds
- [A] Catalog มี `32` รายการ (`27 playable`, `5 metadata-only`) หลังเพิ่ม Magic Lighthouse
- [A] Resurrection ยังคง `metadata-only`; lifecycle skeleton ต้องไม่ attach capability หรือดัก death event
- [A] Magical Shield I-IV มี SP `3/5/8/12`, mana shield 60s และ resistance 30s
- [x] Client: Magical Shield rank IV หัก MP 25% ของ damage แล้วปล่อยส่วนที่เหลือเข้า HP
- [ ] Client: Magical Shield เมื่อ MP ไม่พอและเมื่อ buff หมดเวลา
- [A] Protected Area I-V ใช้ generic timed DR 50%, ระยะเวลา 4-8 วินาที และ self + ally AoE
- [ ] Client: Protected Area rank V ลด final damage ครึ่งหนึ่งและหมดหลัง 8 วินาที
- [ ] Multiplayer: Protected Area ใช้กับ ally ในระยะ 15 blocks สูงสุด 10 คน
- [A] Server startup 2026-07-20 โหลด Skill Definitions `90` definitions / `29` stable IDs
- [A] Damage formula, AoE resolver, links, resources, catalog validation, SP tiers และ production JSON
- [A] Phase 5 SP budget: Fireball `100`, Explosion `225`, Concentrated `125`, Multiple Arrows `50`, Mana Absorption `125`, Lightning `150`, Lightning Chain `205`, Meteor Shower `500`, รวม `1480`
- [A] FG, SA, Iframe, CC budget, Smash, Status และ Mob profile foundation
- [A] Perfect Guard ด้านหน้ากัน damage; ด้านหลังรับ damage แต่ SA กัน CC
- [A] SA/Iframe ยกเลิก Vanilla knockback ผ่าน Forge event
- [ ] Client: อาวุธ Knockback โจมตีระหว่าง SA แล้วไม่ผลัก แต่ damage ยังเข้า
- [ ] Client: TNT/Creeper ด้านหน้า FG กัน damage/impulse; ด้านหลัง FG ไม่กัน
- [ ] Client: FG+SA พร้อมกันแสดง `PerfectGuard=true` ใน `/rpg status`
- [A] Offensive enchant policy ปิด Sharpness/Smite/Bane/Fire Aspect/Knockback/Sweeping ใน RPG Combat
- [ ] Client: Vanilla sword enchant ทำงานเมื่อ sheathed และไม่สร้าง Vanilla hit เมื่อ drawn
- [A] Unknown mod impact fallback เป็น Vanilla และ compatibility event override category/origin ได้
- [A] Mod enchant ID override เป็น `RPG_BRIDGED` ได้โดยไม่เปิด RPG damage formula อัตโนมัติ

ก่อนเริ่ม Manual test รอบใหม่ ให้รัน:

```powershell
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
```

## 2. Third-Person Controls

- [~] Idle: หมุนเมาส์ได้อิสระ ตัวละครไม่หมุนตาม
- [ ] กด WASD: เดินอิงกล้องครบ 8 ทิศและ body หันหา reticle อย่างนุ่มนวล
- [ ] หันกล้องกลับหลังแล้วกด W/S/A/D: ทิศเดินถูกต้องและกล้องไม่ snap
- [ ] กด W+S หรือ A+D พร้อมกัน: ตัวละครไม่เดินและไม่หมุน
- [ ] ปล่อย WASD: body หยุดติดตาม reticle แต่กล้องยังอิสระ
- [~] `Ctrl + RMB` พื้น: click-to-move ถึงจุดหมายโดยกล้องไม่หมุน/ซูม
- [~] `Ctrl + RMB` entity: วิ่งตามตำแหน่งปัจจุบันจนถึงระยะ
- [~] Double `Ctrl + LMB` entity: auto-attack/chase ไม่ทำให้กล้องส่าย
- [ ] คลิกรัวหลายจุด: movement smoothing ไม่เกิดการหันรัว
- [ ] WASD/jump ยกเลิก auto movement ทันที
- [ ] Creative flight ไม่บินวนหลังถึงจุดหมาย
- [ ] น้ำ/ลาวาใช้ Vanilla WASD
- [ ] First Person ยังตี/วางบล็อกได้; RPG Third Person บล็อก interaction ตามกติกา

รายละเอียด tuning: `MOUSE_CONTROL_ACCEPTANCE.md`

## 3. SP Tier And Progression

เริ่มจากโลกทดสอบหรือใช้ `/rpg skills reset` เพื่อไม่ปนข้อมูลเก่า

- [x] `/rpg addskillxp 200000` เพิ่ม SP โดยไม่เพิ่ม Character Level
- [x] Fireball ranks I-IV ใช้ SP `10 + 20 + 30 + 40 = 100`
- [x] Fireball Explosion ranks I-III ใช้ SP `50 + 75 + 100 = 225`
- [x] Concentrated Magic Arrow ranks I-III ใช้ SP `25 + 40 + 60 = 125`
- [A] 27 playable skillsปัจจุบันใช้รวม `3598 SP`
- [x] SP ไม่ติดลบเมื่อแต้มไม่พอ และ rejected upgrade ไม่หัก SP
- [x] Downgrade คืนค่า SP ของ rank นั้น
- [x] `/rpg skills reset` คืน learned ranks/spent SP ถูกต้อง
- [ ] Relog, death และเปลี่ยนมิติไม่ทำ rank/SP หาย
- [ ] `/reload` โหลด tier ใหม่โดย catalog ไม่หายหรือกลายเป็น invalid

Policy: `SKILL_POINT_COST_POLICY.md`

## 4. Fireball And Explosion

- [~] Fireball ยิงตาม aim ไม่ตาม selected target
- [x] Projectile ชน exact entity hitbox และหยุดเมื่อชน block
- [x] Fireball impact ทำ AoE ขนาดเล็กตาม rank
- [x] Explosion ทำ AoE ที่ stored Fireball impact anchor
- [x] Explosion ไม่ย้ายตาม cursor หลัง Fireball ชนแล้ว
- [x] Fireball quick cast: release tick 1, cast 2 ticks, recovery 3 ticks
- [ ] Fireball/Explosion ไม่ถูก WASD หรือ `Ctrl + RMB` cancel หลัง accepted
- [x] MP ไม่พอ: cast ถูกปฏิเสธและ cooldown/link ไม่เปลี่ยน
- [x] Cooldown: cast ซ้ำถูกปฏิเสธโดยไม่เสีย MP เพิ่ม
- [x] Explosion ก่อน Fireball: `MISSING_SKILL_LINK`
- [x] Explosion ครั้งที่สอง: `MISSING_SKILL_LINK` และไม่เสีย MP
- [x] Link หมด 140 ticks: Explosion ถูกปฏิเสธ
- [ ] Relog หลัง Fireball: transient link หาย
- [ ] Fireball เล็งเฉียด hitbox: miss ไม่เกิด hidden aim assist
- [ ] เลือก mob A แต่เล็ง mob B: projectile ไปหา B
- [A] Normal/Elite/Boss/Unstoppable รับ CC/Status ตาม profile; GameTests ครอบคลุม fallback,
  Elite duration, Boss immunity/whitelist และ compatibility override เป็น Unstoppable

คำสั่งเต็ม: `PHASE5_FIREBALL_TEST.md`

## 5. Concentrated Magic Arrow

- [x] Normal `/rpg skills upgrade ...` คืน `PREREQUISITE_REQUIRED` เพราะ Magic Arrow I ยังไม่พร้อม
- [x] Operator `force-upgrade` ข้ามเฉพาะ prerequisite และยังใช้ SP จริง
- [x] Force ranks I-III ใช้รวม `125 SP`
- [x] Rank III: cast 6 ticks, release tick 5, recovery 4 ticks
- [x] Projectile speed 1.5 blocks/tick และชน exact hitbox
- [ ] Impact radius 1.25 blocks โดนไม่เกิน 5 targets
- [ ] Skill Critical bonus 40% รวมกับ caster แล้ว clamp ไม่เกิน 100%
- [~] Knockdown ผ่าน World Core pipeline แล้ว; Down Attack ยังต้องจัดฉากทดสอบ
- [ ] Accepted cast ไม่ถูก WASD หรือ `Ctrl + RMB` cancel
- [ ] Body หันตาม aim จน release แต่กล้องไม่หมุนตาม body
- [~] Cooldown rejection ผ่านแล้ว; MP และ range rejection ยังต้องทดสอบ

หลักฐาน Client 2026-07-18: projectile hit Slime หลายครั้ง, damage `30.72/46.08`,
Knockdown `40 ticks`, cooldown rejection และ block-impact damage ทำงานโดยไม่มี runtime error

หลักฐาน Progression 2026-07-18: Level คงที่ `46` ขณะ SP เพิ่ม `604 -> 804`,
Force Rank I-III ใช้ `25 + 40 + 60 = 125`, downgrade คืน `60` และ reset คืนเป็น
`SP=804 (total=804 spent=0)`

คำสั่งเต็ม: `PHASE5_CONCENTRATED_MAGIC_ARROW_TEST.md`

## 6. Multiple Magic Arrows

- [x] Force upgrade ใช้ `50 SP` และ normal upgrade ต้องการ Magic Arrow V
- [x] Normal cast ใช้ `100 MP`, cast/recovery `5/5 ticks`
- [~] Projectile สองลูกออก tick `3/4` และ AoE โดน 3 เป้าแล้ว; cap 10 ยังไม่ทดสอบ
- [x] Normal cast มี Critical bonus 100%
- [x] Cast ระหว่าง cooldown ได้ด้วย `cooldownRecast=true` และ damage 35%
- [x] Cooldown recast ปิด Critical, Special, CC, Smash, Status, Resource และ Protection
- [ ] Movement ไม่ยกเลิก accepted cast และกล้องไม่หมุนตาม body

คำสั่งเต็ม: `PHASE5_MULTIPLE_MAGIC_ARROWS_TEST.md`

หลักฐาน Client 2026-07-18: normal Critical damage `32.365`, cooldown-recast damage
`7.5519`, projectile สองลูก, `areaTargets=3`, และ MP 89 ถูกปฏิเสธก่อน cast

## 7. Mana Absorption

- [A] Ranks I-III ล็อก cooldown `240/180/140` ticks และ recovery `10/20/30% max MP`
- [A] Cone range 8 blocks, เป้าสูงสุด `4/7/10`, hit windows ticks `8/12`
- [A] Slow 20% นาน 100 ticks และ Front Guard ticks `0-12`
- [x] Rank III ฟื้น `214 MP` จาก Max `716` ครั้งเดียวตอน accepted cast แม้ hit แรกโดน 6 เป้า
- [~] Hit windows ticks `8/12` ทำ damage และ Slow กับ mob ใน cone แล้ว; cap 10 ยังไม่ถูกพิสูจน์
- [x] Cast ซ้ำระหว่าง cooldown ถูกปฏิเสธและไม่ฟื้น MP เพิ่ม
- [ ] ระหว่างร่าย body ตาม aim แต่กล้องไม่หมุนตามตัวละคร

คำสั่งเต็ม: `PHASE5_MANA_ABSORPTION_TEST.md`

หลักฐาน Client 2026-07-18: FG ticks `0-12`, hit windows `8/12`, Slow `100 ticks`
potency `0.2`, cooldown rejection ที่เหลือ `50/16 ticks`, และไม่มี runtime error

## 8. Lightning

- [A] Ranks I-V ล็อก MP `20/30/40/50/60`, cooldown `140/140/120/120/100` ticks และ coefficient ที่ audit แล้ว
- [A] Ground AoE radius 3, range 20, cap 10 และ hit windows ticks `7/8/9`
- [x] Rank V เปิด hit windows `7/8/9`; hit แรกแตะ cap 10 และ Warden เป้าเดิมรับครบสาม hit
- [x] Normal hit แรกทำ Stun `40 ticks`, hit ถัดไปไม่ขอ CC และทุก hit ที่สำเร็จทำ Shocked
- [x] Cooldown recast ทำ base damage `10.7645` จากปกติ `21.5289`, ไม่ทำ Stun แต่ยังทำ Shocked
- [x] Boundary ระยะ 20 บล็อก `STARTED`; ระยะ 25 ถูก `INVALID_TARGET` ซ้ำโดยไม่เริ่ม hit/recovery หรือใช้ MP
- [x] Body หันสู่ anchor โดยกล้องไม่หมุนตาม (Client acceptance 2026-07-19)

คำสั่งเต็ม: `PHASE5_LIGHTNING_TEST.md`

Range acceptance ใช้ `/rpg debug-ground <skill> <whole-block distance>` ซึ่งไม่ใช้ entity
auto-aim; ทดสอบ boundary `20`, `21` และ `25` ตามเอกสารด้านบน

หลักฐาน Client 2026-07-19: Rank V ใช้ MP 60, normal damage `21.5289`, hit แรก
`targets=10`, Warden รับครบสาม hit, Shocked `100 ticks` potency `0.2`, cooldown-recast
ทำ damage `10.7645`; `/rpg debug-ground` ระยะ `20` เริ่มร่ายได้ และระยะ `25`
ถูกปฏิเสธซ้ำโดยมี `target=null` จึงยืนยันว่า range test ไม่ใช้ entity auto-aim
damage `10.7645` (`16.1467` เมื่อ Critical), ไม่มี CC ใน recast และไม่มี runtime error

## 9. Logs To Export

Freeze I-V acceptance evidence (2026-07-19): Rank V consumed `50 MP`, opened hit windows at ticks
`4/5`, applied Freeze on hit two, entered `4` recovery ticks, rejected an immediate recast through
cooldown, and rejected an upgrade beyond Rank V with `MAX_RANK`. The observed Elite-profile Warden
received a reduced Freeze duration of `24 ticks`. The scene contained at most two valid cone targets,
so the Rank V cap of five remains an explicit stress test rather than a claimed manual pass.

Frigid Fog I-IV acceptance evidence (2026-07-19): Rank IV consumed `160 MP`, activated Super Armor
through tick `10`, and opened three `SELF_AOE` windows at ticks `8/9/10`, each resolving exactly ten
targets. Freeze was requested only on the final hit and Elite Wardens either received the expected
`24 ticks` or resisted through their profile. Repeated input was rejected with decreasing cooldown
values and did not create extra hit windows. Resource drain reported `UNSUPPORTED_TARGET` for Wardens
because mobs do not own an MP/WP pool; this is the expected compatibility result, not a skill error.

Residual Lightning Rank IV acceptance evidence (2026-07-19): casting without Lightning returned
`MISSING_SKILL_LINK`; Lightning granted a 100-tick link and stored its impact anchor; Residual consumed
the link and spent `90 MP`. It opened seven ground-AoE windows at ticks `2/3/4/5/6/8/9`, each resolving
ten targets. Bound was requested only by the first window, Slow refreshed on successful hits, and the
two finishers attempted target-resource drain. Wardens correctly returned `UNSUPPORTED_TARGET` because
they do not own an MP/WP pool. An immediate second Residual and explicit cooldown rejection remain
manual checks.

หลังทดสอบ ส่งช่วง log ที่มี tag ต่อไปนี้:

```text
[RPG Skill] acceptance-force-upgrade
[RPG Skill] request
[RPG Skill] result
[RPG Skill] projectile-spawn
[RPG Skill] projectile-hit
[RPG Skill] projectile-end
[RPG Skill] link-grant
[RPG Skill] link-consume
[RPG Skill] damage
```

ถ้า test ไม่ผ่าน ให้จด: คำสั่งที่ใช้, rank, SP/MP ก่อนกด, เป้าหมาย, ระยะโดยประมาณ และผลที่เห็นจริง
เทียบกับ expected result ในหัวข้อเดียวกันก่อนแก้โค้ด

## 10. Completion Gate Before Next Skill

- [ ] Third-person movement/facing รุ่นล่าสุดผ่าน Manual test
- [ ] SP progression และ persistence ผ่าน
- [ ] Fireball rejection/link paths ผ่านครบ
- [ ] Concentrated Magic Arrow acceptance ผ่านครบ
- [ ] ไม่มี error/rejected definition ใน `latest.log`
- [x] Automated baseline ยังเขียวหลังแก้บัคจาก Client test (`test build`, GameTests `14/14`)
- [ ] Mana Absorption acceptance ผ่านครบ
- [ ] Commit และ push acceptance checkpoint

Lightning ซึ่งเป็นสกิลลำดับที่ 6 ของ Batch A ผ่าน runtime/range acceptance แล้ว

## 11. Lightning Chain

- [A] Ranks I-IV, MP, damage, target caps, accuracy, Stiffness และ Shocked ถูกล็อกด้วย production test
- [A] เป้าแรกใช้ exact client aim ray; miss ถูกปฏิเสธก่อนใช้ MP
- [A] Server chain เดินจากเป้าก่อนหน้าในรัศมี 4 บล็อก ไม่เลือก entity ซ้ำต่อ pulse
- [x] Client: Rank IV ชิ่งครบ 6 เป้าทั้ง ticks `5/10` เมื่อจัดต่อกันในระยะ
- [ ] Client: ช่องว่างเกิน 4 บล็อกหยุดสายและไม่ข้ามเป้า
- [x] Client: สอง pulse ticks `5/10`, Stiffness เฉพาะ pulse แรก และ Shocked ทั้งสอง pulse
- [ ] Client: body หันตามเป้าแรกโดยกล้องไม่หมุน
- [x] Client: additional-target damage falloff `1.00/0.85/0.70/0.55/0.40/0.40` ทำ damage ลดจริงทั้งสอง pulse
- [A] Held input จะ request cast ถัดไปหลัง recovery ใน Action Bar phase; ไม่เปลี่ยน two-pulse server contract

คำสั่งเต็ม: `PHASE5_LIGHTNING_CHAIN_TEST.md`

## 12. Earthquake

- [A] Ranks I-IV ล็อก MP `100/140/180/220`, cooldown `2400/2000/1600/1200` ticks,
  hit count `4/5/6/6` และ coefficient `3.0594/3.4986/3.8717/4.5398`
- [A] ทุก Rank เป็น self-AoE radius 6, cap 7, Super Armor และ Down Attack ทุก pulse
- [A] Resource contract รองรับฟื้น MP คงที่ `15` ต่อ hit window ที่สำเร็จ
- [A] CC contract แยก Bound สำหรับ Mob และ Stiffness สำหรับ Player โดยใช้เฉพาะ pulse แรก
- [A] Pull strength `0.18` ไม่ดึง Player, Boss หรือ Unstoppable
- [x] Client: Rank IV เปิดหก hit windows ที่ ticks `4/5/6/7/8/9`
- [x] Client: แต่ละ pulse เลือกเป้าครบ cap 7 โดยไม่เกินเพดาน
- [ ] Client: เป้าด้านใน radius 6 โดน และเป้าด้านนอกไม่โดน
- [ ] Client: Normal/Elite ถูกดึงเข้าหาผู้เล่นอย่างนิ่ง ส่วน Boss/Unstoppable ไม่ถูกดึง
- [x] Client: Wither Boss รับ damage และ pulse แรกปฏิเสธ Bound ด้วย `IMMUNE`, CC points `0`
- [x] Client: Boss Tank อยู่ครบหก windows; 4 HIT/2 MISS ฟื้น MP `60` ตาม successful-hit contract
- [x] Client: ฟื้น MP 15 ต่อ window ครบหกครั้ง รวม 90 และไม่คูณตามจำนวนเป้าหมาย
- [ ] Client: movement cancel ก่อน hit แรก, cooldown rejection และกล้องไม่ขยับ
- [ ] Client: ไม่มี rejected definition, exception หรือ action state ค้างใน `latest.log`

คำสั่งและ expected logs เต็ม: `PHASE6_EARTHQUAKE_TEST.md`

หลักฐาน Client 2026-07-19: Rank IV ใช้ `220 MP`, Super Armor ticks `0-9`, เปิดหก
`SELF_AOE` windows ที่ ticks `4/5/6/7/8/9`, ทุก window มี `targets=7`, และฟื้น MP
`15` หนึ่งครั้งต่อ window รวม `90`. Pulse แรกขอ Bound โดย Elite Warden ได้ duration
`24 ticks` หรือ resist ตาม profile; pulse ถัดไปไม่ขอ CC และ Down Attack ทำงานกับเป้าที่ล้มแล้ว
Cast เข้า recovery `6 ticks` และ complete โดยไม่มี runtime error. Pull visual, radius boundary,
movement cancel, cooldown rejection, Boss/Unstoppable และ Player Stiffness ยังเป็น manual checks.

## 13. Earth's Response

- [x] Rank III ใช้ `A + LMB` และ `D + LMB` ส่ง lateral side `-1/+1` ถูกต้อง
- [x] ระยะ mini-blink ใหม่ข้างละ `2.0` blocks ในพื้นที่ว่าง
- [x] Server collision clipping ลดระยะเมื่อชน โดยไม่ทะลุ block/entity
- [x] Forward line เปิดสอง hit windows ที่ ticks `2/3`
- [x] Hit แรกทำ Floating และ hit ถัดไปเข้า Air Attack เมื่อ CC สำเร็จ
- [x] Cooldown ปฏิเสธ request ซ้ำ และ cast จบ `CASTING -> RECOVERY -> READY`
- [x] Third-person camera ไม่ถูก teleport หรือหมุนตาม body
- [x] ไม่มี skill runtime exception ใน acceptance log

หลักฐาน Client 2026-07-19: `side=1/-1`, `requested=2.0`, actual distance เต็ม `2.0`
ในพื้นที่ว่าง และ `collisionClipped=true` ลด actual distance เหลือประมาณ `1.76-1.96`
เมื่อมีสิ่งกีดขวาง. Rank III ประมวลผลไม่เกิน cap 7 targets ต่อ window ตาม definition.

## 14. Healing Aura

- [A] Ranks I-V ล็อก recovery `12/14/16/18/20%` และ cooldown `15/14/13/12/10s`
- [A] SP ใช้ explicit source values `3/7/12/14/16` รวม `52`
- [A] Heal-only payload ไม่เข้า Damage/Accuracy/Critical/CC pipeline
- [~] Client: Rank V self HP ผ่าน `90.8/454 = 20%`; MP และ maximum clamp ยังไม่มีหลักฐาน
- [ ] Client: optional player ally ในระยะฟื้นพร้อม caster
- [~] Client: เล็ง Bat แล้ว heal เฉพาะ caster ผ่าน; ally นอกระยะ/หลังกำแพงยังไม่ได้ทดสอบ
- [ ] Client: cooldown, relog และ camera/body facing ถูกต้อง

คำสั่งและ expected logs: `PHASE6_HEALING_AURA_TEST.md`

## 15. Healing Lighthouse

- [A] I-IV เป็น channel 3 pulse และแยก self/ally HP recovery
- [A] Rank IV ใช้ MP 220, cooldown 30s, self 30%, ally 20%, self MP 150 ต่อ pulse
- [A] Target cap คือ caster + player allies 10 คน; Mob ไม่เข้า heal resolver
- [A] Explicit SP `5/8/14/18` รวม 45
- [~] Client: 3 pulse, HP 30% และ MP clamp ผ่าน; movement cancel/cooldown ยังไม่ได้ทดสอบ
- [ ] Multiplayer: ally cap, radius และ MP recovery ไม่คูณตามจำนวน ally

คำสั่งและ expected logs: `PHASE6_HEALING_LIGHTHOUSE_TEST.md`
- [ ] Teleport: learn Rank III with `/rpg skill force-upgrade rpg_project:wizard_teleport 3`.
- [ ] Teleport: in Combat Action Mode, `W + Shift + Space` moves forward and `S + Shift + Space` backward.
- [ ] Teleport: press `Shift + Space` without W/S; HUD changes to `TARGETING` and no Stamina/cooldown is consumed yet.
- [ ] Teleport: hold Ctrl and LMB a visible ground point within five seconds to confirm and teleport.
- [ ] Teleport: LMB without Ctrl does not confirm/select an entity; RMB cancels targeting and returns to `READY`.
- [ ] Teleport: targeting timeout returns to `READY` after five seconds without spending Stamina or starting cooldown.
- [ ] Teleport: forged/direct ground cast without a targeting session is rejected with `target confirmation required`.
- [ ] Teleport: destination clamps to 8 blocks and stops before walls/occupied collision boxes.
- [ ] Teleport: each Rank III cast spends 20 Stamina and starts a 7-second cooldown.
- [ ] Teleport: full Iframe is active during ticks 0-4 and camera/yaw do not move with the body.
- [ ] Teleport: successful cast grants +10% Movement Speed for 10 seconds.

## 16. Mouse Targeting Foundation

- [A] Skill definitions support `INSTANT_AIM`, `CONFIRM_TARGETING`, and `CHANNEL_TARGETING`.
- [A] Confirm targeting is server-authoritative, expires once, and consumes resources only after valid confirmation.
- [A] Teleport uses confirm targeting for mouse destinations while W/S directional casts remain immediate.
- [A] Meteor Shower uses channel targeting and accepts server-validated anchor updates during casting only.
- [ ] Client: cast Meteor Shower, then use `Ctrl + LMB` on different ground points before each impact.
- [ ] Client: logs show `retarget` before later `hit-window` entries and later impacts move to the newest anchor.
- [ ] Client: out-of-range retarget logs `retarget-rejected`; earlier valid anchor remains active.
- [ ] Client: channel retarget clicks never select entities or issue click-to-move commands.

## 17. Dagger Stab

- [A] Ranks I-V require both Wizard Main Weapon and Sub-Weapon and cost `0/7/12/14/16` SP.
- [A] Two melee hit windows use coefficients `2.3452/2.5495/2.7386/2.9155/3.0822`.
- [A] Normal cast has Critical `+100%`, Stiffness, Counter Attack, and maximum five targets.
- [x] Client: Rank V produces hit windows at ticks `1/2` against a target within 3 blocks.
- [ ] Client: removing the Wizard Dagger rejects the cast with `INVALID_WEAPON`.
- [x] Client: recast during the 10-second cooldown starts at 35% coefficient without Critical,
  Stiffness, or Counter Attack.
- [ ] Client: body faces the aim on cast while the detached third-person camera remains still.
- [x] Client: no runtime exception or stuck action state appears in `latest.log`.

## 18. Magic Lighthouse

- [A] Ranks I-III lock MP `75/65/55`, cooldown `40/35/30s`, and Core SP `25/40/60`.
- [A] Generic taunt beacon owns duration, refresh interval, radius, cap, placement, and profiles.
- [A] Recast replacement and logout/death/dimension cleanup are server-authoritative.
- [A] Compatibility integrations can cancel retarget through `TauntBeaconTargetEvent`.
- [ ] Client: Rank III spawns one placeholder for 20 seconds, two blocks ahead.
- [ ] Client: Normal/Elite hostile mobs within 12 blocks target it; passive mobs and Boss do not.
- [ ] Client: pulses never exceed ten accepted mobs and do not create duplicate placeholders.
- [ ] Client: expiry/logout/dimension change removes the placeholder and clears runtime state.
- [ ] Client: no exception or stale beacon appears in `latest.log`.
