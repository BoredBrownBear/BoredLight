# VarLightFabric

Fabric Version of [VarLight](https://github.com/flori-schwa/VarLight)

[Core Library](https://github.com/flori-schwa/VarLightCore)

VarLight Allows you to create custom Light sources by right clicking blocks with glowstone dust.

-   Right click blocks with glowstone dust to increment Light Level by 1
-   Left click blocks with glowstone dust to decrement Light level by 1
-   Breaking Custom Light sources with a Silk touch tool will drop a glowing version of the block

Supported MC Versions:

-   1.15.x

Commands:

-   `/varlight update`: Update the light level at a given position (Syntax: `/varlight update <pos> <light level>`)
-   `/varlight fill`: Fill an entire region with Light sources (Syntax: `/varlight fill <pos 1> <pos 2> <light level> [include|exclude] [include filter|exclude filter]`)
-   `/varlight give`: Like the vanilla `/give` command, but receive a glowing version of the block instead
-   `/varlight debug`: List custom light sources
-   `/varlight migrate`: Used when importing a world from a <1.14.2 Spigot server that ran [VarLight](https://github.com/flori-schwa/VarLight)