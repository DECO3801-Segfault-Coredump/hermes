# DECO3801 Hermes Traffic Simulator
**from Team Segmentation fault (core dumped), Semester 2 2023**

## About
This is the simulation engine (Hermes) and the 3D renderer (Atlas) for our DECO3801 project. We
are implementing project 031: Optimised Events – Digital Twins for Traffic Planning.

The Hermes simulator is written in Java and ingests both online and offline Translink GTFS data. The Atlas
renderer is written in Kotlin and uses libGDX as the graphics framework. It ingests OpenStreetMap data and
renders it efficiently into a 3D world. The combined Hermes+Atlas application runs on Windows, Mac and Linux, 
requiring Java 17 or later.

For more information, see the `docs` directory. You will need to at least follow `docs/atlas_map_pipeline.md`
to run the renderer.

**Hermes authors:**
- Lachlan Ellis
- Cathy Nguyen

**Atlas authors:**
- Matt Young
- Henry Batt

**Frontend authors:**
- Connor Vilaysack and Marcus Rehbock wrote the data analytics frontend which is available [here](TODO)

## Building
First, you need to install the dependencies for Hermes and Atlas, which are:

- JDK 17 or later
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

Now, you can open the project in IntelliJ. You should also go to Settings (CTRL+ALT+S) 
-> Build, Execution & Deployment -> Build Tools -> Gradle, and change "Build and run using" _from_ Gradle to
IntelliJ IDEA.

## Running
Run `Lwjgl3Launcher` in the "lwjgl3" subproject, or from the terminal use `./gradlew lwjgl3:run`. Log files
are available in `${HOME}/Documents/DECOSegfault/hermes.log`

## Licence
Unfortunately (or fortunately, depending on who you ask), the University of Queensland owns all the IP to
this project, so you'll have to talk to them.
