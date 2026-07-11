# M-Control (Minecraft RPG Control)

A Minecraft Forge mod designed to overhaul the camera and control system into a modern **Action RPG style** (inspired by Black Desert Online). It seamlessly blends fast-paced action combat camera with hybrid point-and-click movement.

🔗 **Repository:** [https://github.com/kankrittapon/M-Control.git](https://github.com/kankrittapon/M-Control.git)

---

## 🌟 Currently Working Features

* **BDO-Style Independent Camera:** Freely orbit the camera around your character without forcing the character's body to turn.
* **Over-the-Shoulder Perspective:** The camera is offset to the right and slightly up, providing a clear view of the crosshair and enemies.
* **Smooth Camera & Scroll Zoom:** Interpolated camera rotation for a cinematic, non-jittery feel. Supports zooming in/out via the mouse scroll wheel with built-in wall collision detection.
* **Hybrid Control System:** Instantly transition between crosshair-based **Action Mode** and cursor-based **Mouse Mode**.
* **Smart Click-to-Move:** Auto-pathing to clicked destinations. Features intelligent Line-of-Sight (LOS) checks to prevent getting stuck on cliffs, and Auto-mantling (steps over obstacles up to 1.25 blocks high automatically).
* **Two-Step Entity Targeting:** Click an enemy once to lock on (Target Selection + Glow Effect). Click again to issue a move command towards them.
* **Dynamic Crosshair & Floating UI:** Completely replaces the vanilla crosshair with a contextual UI that reacts to what you are aiming at.

---

## 🎮 How to Use (Controls & Menus)

| Key Binding | Action | Description |
| :--- | :--- | :--- |
| **`V`** | **Toggle View Mode** | Cycles through First-person, Vanilla Third-person, and the custom **Action RPG Third-person** camera. |
| **`Hold Left Ctrl`** | **Mouse Mode** | Unlocks the mouse cursor. The camera stops moving, allowing you to click on UI, the ground, or entities. Release to snap back to Action Mode. |
| **`Mouse Scroll`** | **Zoom In/Out** | Adjusts the camera distance dynamically (Only works in Action RPG Third-person mode). |
| **`Ctrl + Left Click (LMB)`** | **Targeting & Attacking** | - **Click & Drag:** Rotate the camera.<br>- **Click Entity (1x):** Select/Lock Target.<br>- **Click Entity (2x):** Auto-Attack Target.<br>- **Click Ground (1x):** Clear Target.<br>- **Click Ground (2x):** Auto-Mine Block. |
| **`Ctrl + Right Click (RMB)`** | **Point & Click Movement** | - **Click Ground:** Walk to the clicked location.<br>- **Click Entity:** Walk to follow the entity. |
| **`R`** | **Cast Magic Bolt** | Fires a magic bolt at the currently selected target (Network packet synced). |

---

## 🎯 Dynamic Target System

To keep the screen clean and allow maximum compatibility with dedicated UI mods (like Neat or Jade), M-Control focuses purely on targeting mechanics:

1. **Dynamic Crosshair (Action Mode):**
   * The vanilla crosshair is hidden. When looking around normally, a minimal white dot is shown.
   * When aiming at an entity, the crosshair transforms into a **Red Combat Reticle**, giving you immediate feedback that you are aiming at a valid target.
2. **Locked Target Aura:**
   * Once an entity is clicked and selected, a distinct **Glowing Aura** is applied to them (Custom render mixin, no potion effects required).
   * This lets you easily track your currently locked target even if they move into a crowd.

---

## 🛠️ Development Log (What We Did)

* **Camera Engine:** Built `ClientCameraController` to decouple yaw/pitch from the player entity. Injected via `CameraMixin` for precise shoulder offsets and safe-distance raycasting.
* **Control State Machine:** Created `ClientControlState` to handle `IDLE`, `MANUAL`, and `NAVIGATING` states. Uses `MovementInputUpdateEvent` to seamlessly inject movement impulses without vanilla keyboard interference.
* **UI Rendering:** Overrode `RenderGuiOverlayEvent.Pre` to disable Vanilla `CROSSHAIR`, replacing it entirely in `RenderGuiOverlayEvent.Post` with our context-aware Dynamic Crosshair.
* **Targeting Logic:** Developed `MouseWorldPicker` and `ClientTargeting` to handle precise 3D raycasting and bounding-box collision for entity selection, allowing a smooth Two-Step Targeting flow.
