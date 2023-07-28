gradle build

java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/asyoulik/asyoulik-gzip.txt.gz ./test/asyoulik/asyoulik-gzip-opt.txt.gz
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/asyoulik/asyoulik-zopfli.txt.gz ./test/asyoulik/asyoulik-zopfli-opt.txt.gz
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/text.png ./test/text-opt.png
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/nerd/nerd.png ./test/nerd/nerd-opt.png
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/nerd/nerd-extopt.png ./test/nerd/nerd-fullopt.png
