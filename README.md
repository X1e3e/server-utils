# Server Utils (RU/EN)

[Русский](#русский) | [English](#english)

---

## Русский

Сборник полезных серверных визуальных фич и утилит для Bukkit/Paper. Позволяет создавать невидимые рамки, настраивать отображение ролей игроков в статус-баре при наведении, гибко управлять видимостью никнеймов и кастомизировать Tab-лист (заголовки, подвалы, пинг и префиксы).

### Фичи
- **Невидимые рамки:** Клик взрывным зельем невидимости по рамке с предметом делает её невидимой. При снятии предмета рамка возвращается в нормальное состояние.
- **Отображение ролей:** Отображает кастомные роли игроков в статус-баре (Action Bar) при наведении курсора.
- **Интеграция с Tab:** Кастомизация Tab-листа с отображением пинга, онлайна, префиксов миров и текущей роли игрока.
- **Автономность ролей:** Вы можете настроить роли независимо от внешних плагинов через сопоставление прав (Permissions) в `config.yml`. Также поддерживается обратная совместимость (fallback) на базу ролей плагина `blockcommand`.

### Настройка конфигурации (config.yml)
```yaml
# Язык по умолчанию: "ru" или "en"
lang: "ru"

# Сопоставление ролей с правами доступа (для отображения в Tab и Action Bar).
# Игроку присваивается первая роль из списка, на которую у него есть право (permission).
role-permissions:
  admin: "serverutils.role.admin"
  detective: "serverutils.role.detective"
  banker: "serverutils.role.banker"
  judge: "serverutils.role.judge"
  mayor: "serverutils.role.mayor"

messages:
  ru:
    invisible_frame_name: "Невидимая рамка"
    tab_title: "Minecraft"
    tab_online: "Онлайн: "
    tab_role: "Ваша роль: "
    tab_ping: "Пинг: "
    tab_ping_ms: " мс"
    roles:
      admin: "§c§lАдминистратор"
      detective: "§bДетектив"
      banker: "§6Банкир"
      judge: "§dСудья"
      mayor: "§aМэр"
      default: "§7Житель"
```

---

## English

A clean and configurable server utility plugin for Bukkit/Paper servers. It adds several visual and utility adjustments to enhance vanilla gameplay, such as invisible item frames, custom Tab lists, hidden name tags, and customizable player roles display.

### Features
- **Invisible Item Frames:** Splash an invisibility potion on a frame to turn it invisible.
- **Role Display:** Shows player roles in the action bar when pointing at them.
- **Tab Integration:** Formats the header and footer of player lists (Tab) to show customized branding, online player counts, player ping, and active roles.
- **Configurable Roles:** Configure roles independently of external plugins by mapping roles to permission nodes in `config.yml`. It also falls back to checking local player roles from `blockcommand` if present.

### Configuration Example (config.yml)
```yaml
lang: "en"

# Map roles to permission nodes. 
# The player will be assigned the first matched role they have permission for.
role-permissions:
  admin: "serverutils.role.admin"
  detective: "serverutils.role.detective"
  banker: "serverutils.role.banker"
  judge: "serverutils.role.judge"
  mayor: "serverutils.role.mayor"

messages:
  en:
    invisible_frame_name: "Invisible Frame"
    tab_title: "Minecraft"
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
