# What is Instantiated?

Instantiated is the newest competitor in the Minecraft dungeon plugin space. 

I made Instantiated because I noticed there were not a lot of options for server owners who wanted custom instanced dungeons for multiple players. With that, I want Instantiated to be able to cater to as many servers as possible.

So, here is what you can do with Instantiated.

## The Do

- ### Instance dungeons for groups of players

    Create an instance of a dungeon for a given group of players, that is completely independent of any other instance, and can be updated at any time with the in-house [edit mode](../usage/editmode.md#the-edit-mode)

    This is achieved by having a master `dungeon format` that all instances use to create their instanced counterparts. 

    For example, if you have a dungeon called `prison` in `dungeons.json` (the data file for Instantiated), any instance of `prison` will use the same format that gets loaded from file. That also means that when the dungeon format changes, any future instances will inherit those changes.