gradle build

java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/asyoulik/asyoulik-gzip.txt.gz ./test/asyoulik/asyoulik-gzip-opt.txt.gz | tee ./test/asyoulik/asyoulik-gzip-opt.txt.gz.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/asyoulik/asyoulik-zopfli.txt.gz ./test/asyoulik/asyoulik-zopfli-opt.txt.gz | tee ./test/asyoulik/asyoulik-zopfli-opt.txt.gz.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/text.png ./test/text-opt.png | tee ./test/text-opt.png.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/apng/ball.png ./test/apng/ball-opt.png | tee ./test/apng/ball-opt.png.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/284-edge-case/284.png ./test/284-edge-case/284-opt.png | tee ./test/284-edge-case/284-opt.png.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/nerd/nerd.png ./test/nerd/nerd-opt.png | tee ./test/nerd/nerd-opt.png.txt
java -ea -jar ./deft4j-cmd/build/libs/deft4j-cmd-all.jar optimise ./test/nerd/nerd-extopt.png ./test/nerd/nerd-fullopt.png | tee ./test/nerd/nerd-fullopt.png.txt
