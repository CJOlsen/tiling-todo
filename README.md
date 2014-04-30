# tiling-todo

A Clojure desktop application built using Dave Ray's Seesaw library to create and organize multiple todo lists.


## Program state

Alpha. This program is not yet finished and includes bugs.  The main functionality is there but behind the scenes a decent amount of code needs to be moved from the View to the Controller and the test suite needs to be finished.


## To use:

Two options: 

Clone the repository and use "lein run" from within the project tree.  Requires Leiningen.

or

Copy target/tiling-todo-0.1.0-alpha-standalone.jar to your local machine, make sure it's in a folder with a "lists/" folder for storage and use "java -jar tiling-todo-0.1.0-alpha-standalone.jar" This option requires Java and has only been lightly tested.


## License

Copyright © 2014 Christopher Olsen

Released under the GNU GPLv3.  Eclipse license available upon request.
