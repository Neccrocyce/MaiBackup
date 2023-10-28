# Changelog
v 2.1 -2023-10-28
--------
#### ADDED:
- option to stop backup, save the progress, and continue at a later point
- Startup option:
  - continue: continues the last backup

### CHANGED:
- command "stop" changed to "stopnow"
- command "stop" stops backup as soon as the current source was backed up completely, and saves the progress.

### FIXED:
- Scanner of the command prompt will not be closed after entering "no" if command "stop" or "stopnow" is prompted.

v 2.0 -2022-03-19
--------
#### ADDED:
- Ignore-option: User can add directories which should not be backed up
- Startup options: 2 new startup options:
    - verbose: more log output
    - options: showing a message dialog when MaiBackup is started
- Settings option timeout for connecting to share
- 2 Console commands shortcuts, s and p 

#### CHANGED:
- Log output
- Program structure. More classes

#### FIXED:
- Possibility to connect to share without using different user credentials


v 1.0
--------