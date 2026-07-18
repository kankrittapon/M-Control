# BRPG Test Checklist

อัปเดต: 2026-07-18

สถานะ:

- `[x]` ผ่านและมีหลักฐาน
- `[~]` เคยผ่านบางส่วน แต่ต้อง Retest หลังการเปลี่ยนล่าสุด
- `[ ]` ยังไม่ได้ทดสอบด้วยผู้เล่น
- `[A]` Automated test ครอบคลุมแล้ว แต่ยังไม่แทนการตรวจ feel ใน Client

## 1. Automated Baseline

- [x] `gradlew test build` ผ่านล่าสุด 2026-07-19 (`55` unit tests)
- [x] `gradlew runGameTestServer` ผ่านล่าสุด 2026-07-19 (`12/12` required GameTests)
- [A] Catalog มี `32` รายการ (`7 playable`, `25 metadata-only`) หลังเพิ่ม Lightning Chain
- [A] Server startup 2026-07-19 โหลด Skill Definitions `28` definitions / `15` stable IDs; Lightning ranks I-V ไม่ถูก reject
- [A] Damage formula, AoE resolver, links, resources, catalog validation, SP tiers และ production JSON
- [A] Phase 5 SP budget: Fireball `100`, Explosion `225`, Concentrated `125`, Multiple Arrows `50`, Mana Absorption `125`, Lightning `150`, Lightning Chain `205`, รวม `980`
- [A] FG, SA, Iframe, CC budget, Smash, Status และ Mob profile foundation

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
- [x] 7 playable skillsปัจจุบันใช้รวม `980 SP`
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
- [ ] Normal/Elite/Boss/Unstoppable รับ CC/Status ตาม profile

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
- [ ] Automated baseline ยังเขียวหลังแก้บัคจาก Client test
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
