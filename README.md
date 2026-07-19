# Server Utils

A clean and configurable server utility plugin for Bukkit/Paper servers. It adds several visual and utility adjustments to enhance vanilla gameplay.

## Features

- **Invisible Item Frames:** Shift-right-click a placed item frame with a splash potion of invisibility in the main hand to turn the item frame invisible. Right-clicking or breaking returns it to normal.
- **Invisible Light Blocks:** Allows players holding a custom permission to interact with and place/view light blocks (`Material.LIGHT`).
- **Action Bar Player Inspect:** Left-clicking or looking at a player displays a stylized action bar displaying their name and role.
- **Hidden Name Tags:** Server-side name tag controller to toggle visibility of player name tags.
- **Custom Tab Lists:** Formats the header and footer of player lists (Tab) to show customized branding, online player counts, player ping, and active roles.

## Configuration

Branding and role names are configured in `config.yml`:

```yaml
lang: "en"

messages:
  en:
    invisible_frame_name: "Invisible Frame"
    tab_title: "My Server"
    tab_online: "Online: "
    tab_role: "Your Role: "
    tab_ping: "Ping: "
    tab_ping_ms: " ms"
    roles:
      admin: "§c§lAdministrator"
      detective: "§bDetective"
      banker: "§6Banker"
      judge: "§dJudge"
      mayor: "§aMayor"
      default: "§7Citizen"
```
