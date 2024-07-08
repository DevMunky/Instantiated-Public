# Commands
    /instantiated:dungeons
    /instantiated:inst
    /instantiated:instantiated

All commands have syntax highlighting and error handling

Any branches without a description implies that there are more arguments needed before executing the command

- ### Command root `dungeons`, `inst`, or `instantiated`
  - ### literal argument `reload`
        
    > Reloads the plugin data and configuration straight from file, without saving!
  
  - ### literal argument `save`
  
    > Saves plugin data (does not affect configuration) as is
  
  - ### literal argument `start`
    - multiple player argument `players`
      - dungeon argument `dungeon`
        - boolean argument `force-create`
              
          > Starts a dungeon containing all of the given players
  
  - ### literal argument `leave`

    > forces the sender of the command to leave the current dungeon, if in one
    
    - multiple player argument `players`

      > forces the given players to leave their current dungeon, if in one
  
  - ### literal argument `edit`

    > puts the sender into [edit mode](../usage/editmode.md#the-edit-mode). This means seeing room and door boundaries, as well as mob spawn locations. Your inventory is also swapped out for the dedicated edit tools.
    
    - dungeon argument `dungeon`
      - literal argument `static` *(only exists to make adding more dungeon types easier)*
        - literal argument `set-schematic`
          - schematic argument `schematic`

            > sets the schematic of the given dungeon
        
        - literal argument `set-spawn`
          - location argument `new-location`

            > sets the spawn location of the given dungeon
        
        - literal argument `add-room`
          - text argument `room-id`

            > adds a blank room with 5 length, 5 width, and 5 height. Further configuration can be done in dungeons.json or with the [ingame edit tool](../usage/editmode.md#the-edit-mode)
        
        - literal argument `remove-room`
          - text argument `room-id`

            > removes the room with the matching room-id