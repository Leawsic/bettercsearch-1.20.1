# BetterCSearch

A Fabric mod that extends [ContainerSearcher](https://modrinth.com/mod/container-searcher) (by Loxoz) to support containers from other mods. Currently adds compatibility for Traveler's Backpack (by Tiviacz) and OrderToCook (by breezeth).

## Features

### Traveler's Backpack Support

- Makes CSearcher recognize Traveler's Backpack blocks as valid containers
- Expands backpack inner inventory items into search results, allowing you to find items stored inside backpacks through CSearcher's search
- Supports both placed backpack blocks and backpack items within other containers

### OrderToCook Order Searching

When holding an OrderItem (order slip) and sneaking, triggers a search for the food items listed on the order:

- Parses the order's FoodList NBT data to determine required items
- Searches all cached containers for matching items by item type (ignores display names, enchantments, and other NBT differences -- renamed items are found correctly)
- All matching containers are highlighted with blinking white outlines
- The first matching item's container is focused: the player's view turns toward it, a particle trail is drawn, and the item is highlighted in the inventory slot overlay
- If multiple items from the same order are in one container, all matching slots are highlighted
- BoardBlocks (menu boards) are excluded from search results to avoid false positives

## Dependencies

- Minecraft 1.20.1
- Fabric Loader >=0.17.3
- Fabric API
- [ContainerSearcher 0.2.1](https://modrinth.com/mod/container-searcher) (included in `libs/`)
- Traveler's Backpack 9.1.41 (included in `libs/`)
- OrderToCook 1.3.2 (included in `libs/`)

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/`.

## Project Structure

```
src/main/java/site/leawsic/bettercsearch/
  BetterCSearch.java          -- Mod initializer
  BetterCSearchClient.java    -- Client initializer (tick event registration)
  mixin/
    MixinCSearcher.java       -- Core mixin: extends CSearcher's container detection
    MixinCSMixinListener.java -- Mixin: captures block interactions for non-BlockWithEntity blocks
  util/
    TravelersBackpackHelper.java -- Reads backpack inner inventory from NBT
    OrderSearchHelper.java       -- OrderItem search logic and container matching
```

### Mixin Details

**MixinCSearcher** (target: `CSearcher`):
- `isHandlerIgnored` -- Whitelists Backpack screen handlers
- `isValidContainer` -- Accepts backpack blocks as valid containers
- `handleContainerScreenUpdate` -- Overwrites the method to expand backpack items when scanning container contents

**MixinCSMixinListener** (target: `CSMixinListener`):
- `onBlockInteract` -- Captures interactions with backpack blocks so they are registered in CSearcher's interaction state

## License

CC0-1.0
