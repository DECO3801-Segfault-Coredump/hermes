# DECO3801 Hermes Traffic Simulator
**from Team Segmentation fault (core dumped), Semester 2 2023**

## About
This is the simulation engine (Hermes) and the 3D renderer (Atlas) for our DECO3801 project. We
are implementing project 031: Optimised Events – Digital Twins for Traffic Planning.

The Hermes simulator is written in Java and ingests both online and offline Translink GTFS data. The Atlas
renderer is written in Kotlin and uses libGDX as the graphics framework. It ingests OpenStreetMap data and
renders it efficiently into a 3D world. The combined Hermes+Atlas application runs on Windows, Mac and Linux, 
requiring Java 17.

For more information, see the `docs` directory. You will need to at least follow `docs/atlas_map_pipeline.md`
to run the renderer.

**Hermes authors:**
- Lachlan Ellis
- Cathy Nguyen

**Atlas authors:**
- Matt Young
- Henry Batt

**Frontend authors:**
- Connor Vilaysack wrote the data analytics frontend which is available [here](https://github.com/DECO3801-Segfault-Coredump/frontend)

## Setting up the toolchain
First, you need to install the dependencies for Hermes and Atlas, which are:

- JDK 17 (exactly this version, no earlier, no later)
  - Windows, Mac: Download from Adoptium: https://adoptium.net/en-GB/
  - Linux: Use your package manager (pacman, apt, etc)
  - Strongly avoid using Oracle's JDK because Matt won't support it and we will have to pay licencing fees :skull:
- Docker
  - Windows: https://docs.docker.com/desktop/install/windows-install/
  - Mac: https://docs.docker.com/desktop/install/mac-install/ (note instructions for Apple Silicon)
  - Linux: Use your package manager (pacman, apt, etc)
- IntelliJ
  - Windows, Mac: Use [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/)
  - Linux: Use JB Toolbox through your package manager if available, otherwise from the website above

**Important:** You now need to set up the OpenStreetMap Docker and import the Brisbane data. 
Please read [docs/atlas_map_pipeline.md](docs/atlas_map_pipeline.md)

## Editing
Now, you can open the project in IntelliJ. You should also go to Settings (CTRL+ALT+S)
-> Build, Execution & Deployment -> Build Tools -> Gradle, and change "Build and run using" _from_ Gradle to
IntelliJ IDEA.

Before writing code, please read [docs/guidelines.md](docs/guidelines.md) for some very loose code style guidelines. Please
remember to add `@author <Your Name>` to each class you write or append your name to the list of authors.

## Running
Run `Lwjgl3Launcher` in the "lwjgl3" subproject. Log files are available in `${HOME}/Documents/DECOSegfault/hermes.log`.
For example, on Linux for me, they are in `/home/matt/Documents/DECOSegfault/hermes.log`.

**Key bindings for Atlas game window:**

- ESCAPE: Quit
- ENTER: Toggle fullscreen
- G: Show/hide debug info
- ]: Force garbage collected caches to run a GC
- P: Print camera pose
- .: Increase Hermes speed
- ,: Decrease Hermes speed
- B: Teleport to random vehicle
- ;: Take screenshot
- WASD: Move forward, left, back, right
- Mouse: Look
- Left click: Select vehicle
- Shift: Boost movement speed
- CTRL: Move down
- SPACE: Zoom up
- R: Reset camera
- T: Show/hide UI adjustments (vehicle boxes and crosshair)
- F: Enable/disable camera following selected vehicle

**Command line options:**

To edit these options: Go to Run -> Edit configurations, go to Lwjgl3Launcher, click "Modify options", then
check "Add VM options". In the field labeled "VM options", paste the arguments.

- `-Ddebug`: Enable debug features (e.g. debug menu shown by default, skip launch prompts)
- `-Dnohermes`: Disable loading Hermes to reduce launch times when working on Atlas graphics
- `-Dnolockmouse`: Disables mouse input capturing, useful for breakpoint debugging
- `-Duvtexturing`: Transitions to `UVTexturingScreen` after load to texture building UVs instead of normal Atlas

**Additional notes**

You can make a release build with `./gradlew lwjgl3:jar`. This will write a runnable JAR file to lwjgl3/build/lib.
This JAR file can be run anywhere with a JRE, and it includes all the app's assets.

It should also be possible to use JPackager to generate bundled native binaries for Windows, Mac and Linux, but
I haven't got around to doing this yet. If it does become necessary ping @matt.

Theoretically you can also run the game using `./gradlew lwjgl3:run`, however there is currently a macOS specific
bug that prevents the game from correctly launching this way in release mode, which is the default. This is
why we recommend running through IntelliJ. On Windows and Linux this is not an issue. If you're on Mac, you
can try pass `-Ddebug` to the game to skip the launch prompts, which may unbreak it. In general though, it
will be easiest to run in IntelliJ.

## Licence
Once the assessment is over, and pending group approval, this project will probably be open-sourced under the
Mozilla Public License v2.0.

Hermes and Atlas use open-source data and 3D models, which are available in 
[atlas_data_raw](https://github.com/DECO3801-Segfault-Coredump/atlas_data_raw).
The following copyright applies to them:

**Attribution**

- Translink GTFS data by Translink
    - CC Attribution
    - https://translink.com.au/about-translink/open-data
- OpenStreetMap by OpenStreetMap contributors
    - Open Data Commons Open Database License
    - https://www.openstreetmap.org/copyright
- Bus model by Jotrain
    - CC Attribution-NonCommercial-NoDerivs
    - https://sketchfab.com/3d-models/brisbane-city-scania-l94ub-bus-rhd-8f41b49ba5344d3a8391a4c0d144d8e8
    - TODO see if we can get a derivs licence to strip the interior
- EMU train model by Jotrain
    - CC Attribution-NonCommercial
    - https://sketchfab.com/3d-models/queensland-rail-emu-low-poly-47f3a898ef624ec59bc29b0a3f6c23c1
- Tow Boat (ferry model) by BoatUS Foundation
    - CC Attribution
    - https://sketchfab.com/3d-models/tow-boat-86939cf48b914951aa3c6ed2bc8bb446
