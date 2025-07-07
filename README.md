# AutoMinerMod

![Minecraft Version](https://img.shields.io/badge/minecraft-1.21.4-blue.svg)
![Mod Loader](https://img.shields.io/badge/loader-Fabric-green.svg)
![License](https://img.shields.io/badge/license-CC0--1.0-lightgrey.svg)

**AutoMinerMod** is a Fabric mod for Minecraft that automatically mines a selected 3D area, layer by layer from top to bottom. Once the area is defined, the mod handles navigation, block targeting, breaking, and tool switching autonomously.

## Features

- Automatically mines all blocks in a selected cuboid area
- Starts from the top layer and goes downward
- Avoids air and bedrock blocks
- Dynamically selects the most effective tool
- Walks the player to adjacent blocks for mining
- Prevents falling by placing solid blocks when needed
- Displays mining progress and estimated time left on HUD

## Requirements

- Minecraft `1.21.4`
- Fabric Loader `>= 0.16.14`
- Fabric API
- Java `21`

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/).
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the latest release of this mod from [Releases](https://github.com/cornel36/AutoMinerMod/releases).
4. Put the `.jar` file into your `.minecraft/mods/` folder.
5. Launch Minecraft with the Fabric profile.

## Building from Source

git clone https://github.com/cornel36/AutoMinerMod.git  

cd AutoMinerMod  

./gradlew build  

After building, the output .jar can be found in the build/libs/ directory!!!!  

## Usage
1. Select two corners of the area you want to mine using a wooden sword (left click - position 1, right click - position 2).
2. Click "K" on keyboard to start/stop the mod.
3. The mod will begin mining blocks layer by layer.
4. The player will automatically walk, mine, and manage tools.

## License
This mod is released under the CC0-1.0 License.
You can use, modify, and distribute it freely.

## Author
Created and maintained by cornel36.
Feel free to submit issues, suggestions, or pull requests!
