# deft4j

> WIP deflate toolkit for Java.

deft4j is a (very inefficient) deflate stream parser and optimiser.

It has support for parsing and writing ZIP, GZip, ZLib, and PNG files.

deft4j implements a few deflate stream optimisations:

- If the encoded size of a backref is larger than the equivalent sequence of literals, and the literals are able to be encoded by the huffman codes a block uses, it replaces the backref with literals.
- Similarly, if a RLE instruction in a dynamic tree header is larger than the equivalent sequence of literals, it replaces the instruction with literals.
- An attempt at re-encoding dynamic headers is made, although this should be improved in the future.
- Type 2 blocks are tested to see if they would be smaller when encoded as type 1 blocks.

Future work includes:
- Merging type 0 and 1 blocks, as well as type 2 blocks which have the same huffman trees.
- Improvements to header re-encoding.
- Backref replacement when the symbol is not already able to be encoded.

deft4j also includes options for recompressing files with various libraries, including [CafeUndZopfli](https://github.com/eustas/CafeUndZopfli).

## Usage

deft4j is currently available as a command line application:

```
Usage: deft4j [-hrV] [-f=<format>] [-m=<recompressMode>] <inputFile>
              <outputFile>
Deflate stream optimiser
      <inputFile>         The file to optimise
      <outputFile>        The optimised file
  -f, --format=<format>   File format
  -h, --help              Show this help message and exit.
  -m, --mode, --recompress-mode=<recompressMode>
                          Enable various levels of recompression. Valid values:
                            NONE, CHEAP, ZOPFLI, ZOPFLI_EXTENSIVE
  -r, --raw               Ignore file format, treat input as a raw deflate
                            stream
  -V, --version           Print version information and exit.
```

## Credits

The code for parsing and writing deflate streams is based on [hwzip](https://www.hanshq.net/zip.html), which is in the public domain.
The code for Huffman encoding and decoding is largely from [deflate-impl](https://github.com/RidgeX/deflate-impl), which is licensed under the MIT license.

## License

All original code is licensed under the BSD Zero Clause License. The code for Huffman encoding and decoding is licensed under its original terms of the MIT license.
