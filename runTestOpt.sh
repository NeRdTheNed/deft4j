gradle build

java -ea -jar ./build/libs/deft4j-all.jar ./test/asyoulik/asyoulik-gzip.txt.gz ./test/asyoulik/asyoulik-gzip-opt.txt.gz
java -ea -jar ./build/libs/deft4j-all.jar ./test/asyoulik/asyoulik-zopfli.txt.gz ./test/asyoulik/asyoulik-zopfli-opt.txt.gz
java -ea -jar ./build/libs/deft4j-all.jar ./test/nerd/nerd.png ./test/nerd/nerd-opt.png
java -ea -jar ./build/libs/deft4j-all.jar ./test/nerd/nerd-extopt.png ./test/nerd/nerd-fullopt.png
