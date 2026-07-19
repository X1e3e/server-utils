# Server Utils (RU/EN)

[Русский](#русский) | [English](#english)

---

## Русский

Сборник полезных серверных визуальных фич, кастомных рецептов и утилит для Bukkit/Paper.

### Фичи
- **Невидимые рамки:** Клик взрывным зельем невидимости по рамке с предметом делает её невидимой.
- **Стучание в двери:** Возможность «постучать» в деревянную дверь (Shift + ЛКМ).
- **Ремонт наковален:** Быстрый ремонт поврежденной наковальни при помощи железного блока (Shift + ПКМ).
- **Подпись предметов:** Подпись любого предмета при помощи пера (Shift + ПКМ).
- **Защита артов:** Защита заблокированных артов (карт), книг и шаблонов от копирования другими игроками.
- **Изменение размера:** Команда `/scale` для изменения размера модельки игрока.
- **Кастомные рецепты:** Возможность создавать собственные Shaped рецепты крафта с настраиваемыми именами и описаниями результатов через `config.yml`.
- **Кастомизация Tab:** Настройка заголовков, подвалов, пинга, ролей и префиксов миров.

### Команды
- `/scale <размер от 0.9 до 1.1>` — Изменить размер модельки.
- `/lock` — Заблокировать карту/книгу от копирования.
- `/unlock` — Разблокировать карту/книгу.
- `/serverutils reload` — Перезагрузить конфигурацию плагина (право `serverutils.admin`).

### Настройка конфигурации (config.yml)
```yaml
# Язык по умолчанию: "ru" или "en" (файлы локализации находятся в lang/)
lang: "ru"

# Включение и отключение отдельных функций плагина
features:
  elytra-end-restriction: true     # Запрет полетов на элитрах в Энде
  anvil-repair: true               # Ремонт наковален железными блоками
  door-knocking: true              # Стук в деревянные двери (Shift + ЛКМ)
  enderman-pickup-protection: true # Запрет эндерменам переносить блоки
  item-signing: true               # Подпись предметов пером (Shift + ПКМ)
  item-lock-protection: true       # Защита заблокированных артов/книг от копирования
  tab-formatter: true              # Форматирование списка игроков (Tab)
  hide-nametags: true              # Скрытие никнеймов над головой игроков
  recipes:
    invisible-frame: true          # Крафт невидимой рамки
    invisible-light: true          # Крафт невидимого источника света
    debug-stick: true              # Крафт отладочной палки (Debug Stick)

# Сопоставление ролей с правами доступа (для отображения в Tab и Action Bar).
role-permissions:
  admin: "serverutils.role.admin"
  detective: "serverutils.role.detective"
  banker: "serverutils.role.banker"
  judge: "serverutils.role.judge"
  mayor: "serverutils.role.mayor"

# Кастомные Shaped рецепты крафта
custom-recipes:
  saddle:
    enabled: true
    result:
      type: "SADDLE"
      amount: 1
      name: "&eСамодельное седло"
      lore:
        - "&7Создано из качественной кожи"
        - "&7и прочных нитей."
    shape:
      - "LLL"
      - "S S"
      - "   "
    ingredients:
      L: "LEATHER"
      S: "STRING"
```

---

## English

A clean and configurable server utility and custom recipe plugin for Bukkit/Paper servers.

### Features
- **Invisible Item Frames:** Splash an invisibility potion on a frame to turn it invisible.
- **Door Knocking:** Knock on wooden doors (Shift + Left Click).
- **Anvil Repair:** Repair chipped/damaged anvils using an iron block (Shift + Right Click).
- **Item Signing:** Sign any item with a feather (Shift + Right Click).
- **Art Protection:** Protect locked maps (arts), written books, and templates from copying by others.
- **Player Scaling:** Command `/scale` to change player model scale size.
- **Custom Recipes:** Register custom Shaped crafting recipes with names and lore via `config.yml`.
- **Tab Customization:** Format header/footer, ping, world prefixes, and player roles.

### Commands
- `/scale <size from 0.9 to 1.1>` — Change your size.
- `/lock` — Lock a map/book from being copied.
- `/unlock` — Unlock a map/book.
- `/serverutils reload` — Reload the configuration (permission `serverutils.admin`).

### Configuration Example (config.yml)
```yaml
lang: "en"

features:
  elytra-end-restriction: true     # Disable elytra gliding in the End
  anvil-repair: true               # Repair anvils with iron blocks
  door-knocking: true              # Knock on wooden doors (Shift + Left Click)
  enderman-pickup-protection: true # Stop Endermen from picking up blocks
  item-signing: true               # Sign items with a feather (Shift + Right Click)
  item-lock-protection: true       # Protect locked maps/books from copying
  tab-formatter: true              # Format player lists (Tab)
  hide-nametags: true              # Hide name tags above player heads
  recipes:
    invisible-frame: true          # Recipe for invisible frames
    invisible-light: true          # Recipe for invisible light blocks
    debug-stick: true              # Recipe for debug sticks

role-permissions:
  admin: "serverutils.role.admin"
  detective: "serverutils.role.detective"
  banker: "serverutils.role.banker"
  judge: "serverutils.role.judge"
  mayor: "serverutils.role.mayor"

custom-recipes:
  saddle:
    enabled: true
    result:
      type: "SADDLE"
      amount: 1
      name: "&eHandmade Saddle"
      lore:
        - "&7Crafted using leather"
        - "&7and strong threads."
    shape:
      - "LLL"
      - "S S"
      - "   "
    ingredients:
      L: "LEATHER"
      S: "STRING"
```
