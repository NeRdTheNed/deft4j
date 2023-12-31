package com.github.NeRdTheNed.deft4j.deflate;

public final class Constants {
    /** Private constructor to hide the default one */
    private Constants() {
        // This space left intentionally blank
    }

    public static int distance2dist(long distance) {
        assert (distance >= 1) && (distance <= Constants.MAX_DISTANCE);
        return distance <= 256 ? Constants.distance2dist_lo[(int) (distance - 1)]
               : Constants.distance2dist_hi[(int) ((distance - 1) >> 7)];
    }

    public static int len2litlen(int len, boolean edgecase) {
        assert !edgecase || (len == Constants.MAX_LEN);

        if (edgecase) {
            return 284;
        }

        return len2litlen[len];
    }

    public static final int LZZ_BACKREF_LEN = 32 * 1024;

    public static final int LITLEN_MAX = 285;
    public static final int LITLEN_TBL_OFFSET = 257;

    public static final int MIN_LEN = 3;
    public static final int MAX_LEN = 258;

    public static final int DISTSYM_MAX = 29;

    public static final int MIN_DISTANCE = 1;
    public static final int MAX_DISTANCE = 32768;

    public static final int LITLEN_EOB = 256;

    public static final int MIN_CODELEN_LENS = 4;
    public static final int MAX_CODELEN_LENS = 19;

    public static final int MIN_LITLEN_LENS = 257;
    public static final int MAX_LITLEN_LENS = 288;

    public static final int MIN_DIST_LENS = 1;
    public static final int MAX_DIST_LENS = 32;

    public static final int CODELEN_MAX_LIT = 15;

    public static final int CODELEN_COPY = 16;
    public static final int CODELEN_COPY_MIN = 3;
    public static final int CODELEN_COPY_MAX = 6;

    public static final int CODELEN_ZEROS = 17;
    public static final int CODELEN_ZEROS_MIN = 3;
    public static final int CODELEN_ZEROS_MAX = 10;

    public static final int CODELEN_ZEROS2 = 18;
    public static final int CODELEN_ZEROS2_MIN = 11;
    public static final int CODELEN_ZEROS2_MAX = 138;

    static final int[] codelen_lengths_order = { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };

    static final LengthPair[] litlen_tbl = {
        /* 257 */ new LengthPair( 3, 0 ),
        /* 258 */ new LengthPair( 4, 0 ),
        /* 259 */ new LengthPair( 5, 0 ),
        /* 260 */ new LengthPair( 6, 0 ),
        /* 261 */ new LengthPair( 7, 0 ),
        /* 262 */ new LengthPair( 8, 0 ),
        /* 263 */ new LengthPair( 9, 0 ),
        /* 264 */ new LengthPair( 10, 0 ),
        /* 265 */ new LengthPair( 11, 1 ),
        /* 266 */ new LengthPair( 13, 1 ),
        /* 267 */ new LengthPair( 15, 1 ),
        /* 268 */ new LengthPair( 17, 1 ),
        /* 269 */ new LengthPair( 19, 2 ),
        /* 270 */ new LengthPair( 23, 2 ),
        /* 271 */ new LengthPair( 27, 2 ),
        /* 272 */ new LengthPair( 31, 2 ),
        /* 273 */ new LengthPair( 35, 3 ),
        /* 274 */ new LengthPair( 43, 3 ),
        /* 275 */ new LengthPair( 51, 3 ),
        /* 276 */ new LengthPair( 59, 3 ),
        /* 277 */ new LengthPair( 67, 4 ),
        /* 278 */ new LengthPair( 83, 4 ),
        /* 279 */ new LengthPair( 99, 4 ),
        /* 280 */ new LengthPair( 115, 4 ),
        /* 281 */ new LengthPair( 131, 5 ),
        /* 282 */ new LengthPair( 163, 5 ),
        /* 283 */ new LengthPair( 195, 5 ),
        /* 284 */ new LengthPair( 227, 5 ),
        /* 285 */ new LengthPair( 258, 0 ),
    };

    static final DistancePair[] dist_tbl = {
        /*  0 */ new DistancePair( 1, 0 ),
        /*  1 */ new DistancePair( 2, 0 ),
        /*  2 */ new DistancePair( 3, 0 ),
        /*  3 */ new DistancePair( 4, 0 ),
        /*  4 */ new DistancePair( 5, 1 ),
        /*  5 */ new DistancePair( 7, 1 ),
        /*  6 */ new DistancePair( 9, 2 ),
        /*  7 */ new DistancePair( 13, 2 ),
        /*  8 */ new DistancePair( 17, 3 ),
        /*  9 */ new DistancePair( 25, 3 ),
        /* 10 */ new DistancePair( 33, 4 ),
        /* 11 */ new DistancePair( 49, 4 ),
        /* 12 */ new DistancePair( 65, 5 ),
        /* 13 */ new DistancePair( 97, 5 ),
        /* 14 */ new DistancePair( 129, 6 ),
        /* 15 */ new DistancePair( 193, 6 ),
        /* 16 */ new DistancePair( 257, 7 ),
        /* 17 */ new DistancePair( 385, 7 ),
        /* 18 */ new DistancePair( 513, 8 ),
        /* 19 */ new DistancePair( 769, 8 ),
        /* 20 */ new DistancePair( 1025, 9 ),
        /* 21 */ new DistancePair( 1537, 9 ),
        /* 22 */ new DistancePair( 2049, 10 ),
        /* 23 */ new DistancePair( 3073, 10 ),
        /* 24 */ new DistancePair( 4097, 11 ),
        /* 25 */ new DistancePair( 6145, 11 ),
        /* 26 */ new DistancePair( 8193, 12 ),
        /* 27 */ new DistancePair( 12289, 12 ),
        /* 28 */ new DistancePair( 16385, 13 ),
        /* 29 */ new DistancePair( 24577, 13 ),
    };

    private static final int[] len2litlen = {
        /*   0 */ 0xffff,
        /*   1 */ 0xffff,
        /*   2 */ 0xffff,
        /*   3 */ 257,
        /*   4 */ 258,
        /*   5 */ 259,
        /*   6 */ 260,
        /*   7 */ 261,
        /*   8 */ 262,
        /*   9 */ 263,
        /*  10 */ 264,
        /*  11 */ 265,
        /*  12 */ 265,
        /*  13 */ 266,
        /*  14 */ 266,
        /*  15 */ 267,
        /*  16 */ 267,
        /*  17 */ 268,
        /*  18 */ 268,
        /*  19 */ 269,
        /*  20 */ 269,
        /*  21 */ 269,
        /*  22 */ 269,
        /*  23 */ 270,
        /*  24 */ 270,
        /*  25 */ 270,
        /*  26 */ 270,
        /*  27 */ 271,
        /*  28 */ 271,
        /*  29 */ 271,
        /*  30 */ 271,
        /*  31 */ 272,
        /*  32 */ 272,
        /*  33 */ 272,
        /*  34 */ 272,
        /*  35 */ 273,
        /*  36 */ 273,
        /*  37 */ 273,
        /*  38 */ 273,
        /*  39 */ 273,
        /*  40 */ 273,
        /*  41 */ 273,
        /*  42 */ 273,
        /*  43 */ 274,
        /*  44 */ 274,
        /*  45 */ 274,
        /*  46 */ 274,
        /*  47 */ 274,
        /*  48 */ 274,
        /*  49 */ 274,
        /*  50 */ 274,
        /*  51 */ 275,
        /*  52 */ 275,
        /*  53 */ 275,
        /*  54 */ 275,
        /*  55 */ 275,
        /*  56 */ 275,
        /*  57 */ 275,
        /*  58 */ 275,
        /*  59 */ 276,
        /*  60 */ 276,
        /*  61 */ 276,
        /*  62 */ 276,
        /*  63 */ 276,
        /*  64 */ 276,
        /*  65 */ 276,
        /*  66 */ 276,
        /*  67 */ 277,
        /*  68 */ 277,
        /*  69 */ 277,
        /*  70 */ 277,
        /*  71 */ 277,
        /*  72 */ 277,
        /*  73 */ 277,
        /*  74 */ 277,
        /*  75 */ 277,
        /*  76 */ 277,
        /*  77 */ 277,
        /*  78 */ 277,
        /*  79 */ 277,
        /*  80 */ 277,
        /*  81 */ 277,
        /*  82 */ 277,
        /*  83 */ 278,
        /*  84 */ 278,
        /*  85 */ 278,
        /*  86 */ 278,
        /*  87 */ 278,
        /*  88 */ 278,
        /*  89 */ 278,
        /*  90 */ 278,
        /*  91 */ 278,
        /*  92 */ 278,
        /*  93 */ 278,
        /*  94 */ 278,
        /*  95 */ 278,
        /*  96 */ 278,
        /*  97 */ 278,
        /*  98 */ 278,
        /*  99 */ 279,
        /* 100 */ 279,
        /* 101 */ 279,
        /* 102 */ 279,
        /* 103 */ 279,
        /* 104 */ 279,
        /* 105 */ 279,
        /* 106 */ 279,
        /* 107 */ 279,
        /* 108 */ 279,
        /* 109 */ 279,
        /* 110 */ 279,
        /* 111 */ 279,
        /* 112 */ 279,
        /* 113 */ 279,
        /* 114 */ 279,
        /* 115 */ 280,
        /* 116 */ 280,
        /* 117 */ 280,
        /* 118 */ 280,
        /* 119 */ 280,
        /* 120 */ 280,
        /* 121 */ 280,
        /* 122 */ 280,
        /* 123 */ 280,
        /* 124 */ 280,
        /* 125 */ 280,
        /* 126 */ 280,
        /* 127 */ 280,
        /* 128 */ 280,
        /* 129 */ 280,
        /* 130 */ 280,
        /* 131 */ 281,
        /* 132 */ 281,
        /* 133 */ 281,
        /* 134 */ 281,
        /* 135 */ 281,
        /* 136 */ 281,
        /* 137 */ 281,
        /* 138 */ 281,
        /* 139 */ 281,
        /* 140 */ 281,
        /* 141 */ 281,
        /* 142 */ 281,
        /* 143 */ 281,
        /* 144 */ 281,
        /* 145 */ 281,
        /* 146 */ 281,
        /* 147 */ 281,
        /* 148 */ 281,
        /* 149 */ 281,
        /* 150 */ 281,
        /* 151 */ 281,
        /* 152 */ 281,
        /* 153 */ 281,
        /* 154 */ 281,
        /* 155 */ 281,
        /* 156 */ 281,
        /* 157 */ 281,
        /* 158 */ 281,
        /* 159 */ 281,
        /* 160 */ 281,
        /* 161 */ 281,
        /* 162 */ 281,
        /* 163 */ 282,
        /* 164 */ 282,
        /* 165 */ 282,
        /* 166 */ 282,
        /* 167 */ 282,
        /* 168 */ 282,
        /* 169 */ 282,
        /* 170 */ 282,
        /* 171 */ 282,
        /* 172 */ 282,
        /* 173 */ 282,
        /* 174 */ 282,
        /* 175 */ 282,
        /* 176 */ 282,
        /* 177 */ 282,
        /* 178 */ 282,
        /* 179 */ 282,
        /* 180 */ 282,
        /* 181 */ 282,
        /* 182 */ 282,
        /* 183 */ 282,
        /* 184 */ 282,
        /* 185 */ 282,
        /* 186 */ 282,
        /* 187 */ 282,
        /* 188 */ 282,
        /* 189 */ 282,
        /* 190 */ 282,
        /* 191 */ 282,
        /* 192 */ 282,
        /* 193 */ 282,
        /* 194 */ 282,
        /* 195 */ 283,
        /* 196 */ 283,
        /* 197 */ 283,
        /* 198 */ 283,
        /* 199 */ 283,
        /* 200 */ 283,
        /* 201 */ 283,
        /* 202 */ 283,
        /* 203 */ 283,
        /* 204 */ 283,
        /* 205 */ 283,
        /* 206 */ 283,
        /* 207 */ 283,
        /* 208 */ 283,
        /* 209 */ 283,
        /* 210 */ 283,
        /* 211 */ 283,
        /* 212 */ 283,
        /* 213 */ 283,
        /* 214 */ 283,
        /* 215 */ 283,
        /* 216 */ 283,
        /* 217 */ 283,
        /* 218 */ 283,
        /* 219 */ 283,
        /* 220 */ 283,
        /* 221 */ 283,
        /* 222 */ 283,
        /* 223 */ 283,
        /* 224 */ 283,
        /* 225 */ 283,
        /* 226 */ 283,
        /* 227 */ 284,
        /* 228 */ 284,
        /* 229 */ 284,
        /* 230 */ 284,
        /* 231 */ 284,
        /* 232 */ 284,
        /* 233 */ 284,
        /* 234 */ 284,
        /* 235 */ 284,
        /* 236 */ 284,
        /* 237 */ 284,
        /* 238 */ 284,
        /* 239 */ 284,
        /* 240 */ 284,
        /* 241 */ 284,
        /* 242 */ 284,
        /* 243 */ 284,
        /* 244 */ 284,
        /* 245 */ 284,
        /* 246 */ 284,
        /* 247 */ 284,
        /* 248 */ 284,
        /* 249 */ 284,
        /* 250 */ 284,
        /* 251 */ 284,
        /* 252 */ 284,
        /* 253 */ 284,
        /* 254 */ 284,
        /* 255 */ 284,
        /* 256 */ 284,
        /* 257 */ 284,
        /* 258 */ 285,
    };

    private static final int[] distance2dist_lo = {
        0, /*   1 */
        1, /*   2 */
        2, /*   3 */
        3, /*   4 */
        4, /*   5 */
        4, /*   6 */
        5, /*   7 */
        5, /*   8 */
        6, /*   9 */
        6, /*  10 */
        6, /*  11 */
        6, /*  12 */
        7, /*  13 */
        7, /*  14 */
        7, /*  15 */
        7, /*  16 */
        8, /*  17 */
        8, /*  18 */
        8, /*  19 */
        8, /*  20 */
        8, /*  21 */
        8, /*  22 */
        8, /*  23 */
        8, /*  24 */
        9, /*  25 */
        9, /*  26 */
        9, /*  27 */
        9, /*  28 */
        9, /*  29 */
        9, /*  30 */
        9, /*  31 */
        9, /*  32 */
        10, /*  33 */
        10, /*  34 */
        10, /*  35 */
        10, /*  36 */
        10, /*  37 */
        10, /*  38 */
        10, /*  39 */
        10, /*  40 */
        10, /*  41 */
        10, /*  42 */
        10, /*  43 */
        10, /*  44 */
        10, /*  45 */
        10, /*  46 */
        10, /*  47 */
        10, /*  48 */
        11, /*  49 */
        11, /*  50 */
        11, /*  51 */
        11, /*  52 */
        11, /*  53 */
        11, /*  54 */
        11, /*  55 */
        11, /*  56 */
        11, /*  57 */
        11, /*  58 */
        11, /*  59 */
        11, /*  60 */
        11, /*  61 */
        11, /*  62 */
        11, /*  63 */
        11, /*  64 */
        12, /*  65 */
        12, /*  66 */
        12, /*  67 */
        12, /*  68 */
        12, /*  69 */
        12, /*  70 */
        12, /*  71 */
        12, /*  72 */
        12, /*  73 */
        12, /*  74 */
        12, /*  75 */
        12, /*  76 */
        12, /*  77 */
        12, /*  78 */
        12, /*  79 */
        12, /*  80 */
        12, /*  81 */
        12, /*  82 */
        12, /*  83 */
        12, /*  84 */
        12, /*  85 */
        12, /*  86 */
        12, /*  87 */
        12, /*  88 */
        12, /*  89 */
        12, /*  90 */
        12, /*  91 */
        12, /*  92 */
        12, /*  93 */
        12, /*  94 */
        12, /*  95 */
        12, /*  96 */
        13, /*  97 */
        13, /*  98 */
        13, /*  99 */
        13, /* 100 */
        13, /* 101 */
        13, /* 102 */
        13, /* 103 */
        13, /* 104 */
        13, /* 105 */
        13, /* 106 */
        13, /* 107 */
        13, /* 108 */
        13, /* 109 */
        13, /* 110 */
        13, /* 111 */
        13, /* 112 */
        13, /* 113 */
        13, /* 114 */
        13, /* 115 */
        13, /* 116 */
        13, /* 117 */
        13, /* 118 */
        13, /* 119 */
        13, /* 120 */
        13, /* 121 */
        13, /* 122 */
        13, /* 123 */
        13, /* 124 */
        13, /* 125 */
        13, /* 126 */
        13, /* 127 */
        13, /* 128 */
        14, /* 129 */
        14, /* 130 */
        14, /* 131 */
        14, /* 132 */
        14, /* 133 */
        14, /* 134 */
        14, /* 135 */
        14, /* 136 */
        14, /* 137 */
        14, /* 138 */
        14, /* 139 */
        14, /* 140 */
        14, /* 141 */
        14, /* 142 */
        14, /* 143 */
        14, /* 144 */
        14, /* 145 */
        14, /* 146 */
        14, /* 147 */
        14, /* 148 */
        14, /* 149 */
        14, /* 150 */
        14, /* 151 */
        14, /* 152 */
        14, /* 153 */
        14, /* 154 */
        14, /* 155 */
        14, /* 156 */
        14, /* 157 */
        14, /* 158 */
        14, /* 159 */
        14, /* 160 */
        14, /* 161 */
        14, /* 162 */
        14, /* 163 */
        14, /* 164 */
        14, /* 165 */
        14, /* 166 */
        14, /* 167 */
        14, /* 168 */
        14, /* 169 */
        14, /* 170 */
        14, /* 171 */
        14, /* 172 */
        14, /* 173 */
        14, /* 174 */
        14, /* 175 */
        14, /* 176 */
        14, /* 177 */
        14, /* 178 */
        14, /* 179 */
        14, /* 180 */
        14, /* 181 */
        14, /* 182 */
        14, /* 183 */
        14, /* 184 */
        14, /* 185 */
        14, /* 186 */
        14, /* 187 */
        14, /* 188 */
        14, /* 189 */
        14, /* 190 */
        14, /* 191 */
        14, /* 192 */
        15, /* 193 */
        15, /* 194 */
        15, /* 195 */
        15, /* 196 */
        15, /* 197 */
        15, /* 198 */
        15, /* 199 */
        15, /* 200 */
        15, /* 201 */
        15, /* 202 */
        15, /* 203 */
        15, /* 204 */
        15, /* 205 */
        15, /* 206 */
        15, /* 207 */
        15, /* 208 */
        15, /* 209 */
        15, /* 210 */
        15, /* 211 */
        15, /* 212 */
        15, /* 213 */
        15, /* 214 */
        15, /* 215 */
        15, /* 216 */
        15, /* 217 */
        15, /* 218 */
        15, /* 219 */
        15, /* 220 */
        15, /* 221 */
        15, /* 222 */
        15, /* 223 */
        15, /* 224 */
        15, /* 225 */
        15, /* 226 */
        15, /* 227 */
        15, /* 228 */
        15, /* 229 */
        15, /* 230 */
        15, /* 231 */
        15, /* 232 */
        15, /* 233 */
        15, /* 234 */
        15, /* 235 */
        15, /* 236 */
        15, /* 237 */
        15, /* 238 */
        15, /* 239 */
        15, /* 240 */
        15, /* 241 */
        15, /* 242 */
        15, /* 243 */
        15, /* 244 */
        15, /* 245 */
        15, /* 246 */
        15, /* 247 */
        15, /* 248 */
        15, /* 249 */
        15, /* 250 */
        15, /* 251 */
        15, /* 252 */
        15, /* 253 */
        15, /* 254 */
        15, /* 255 */
        15, /* 256 */
    };

    private static final int[] distance2dist_hi = {
        0xff, /* invalid */
        0xff, /* invalid */
        16, /*   257-- */
        17, /*   385-- */
        18, /*   513-- */
        18, /*   641-- */
        19, /*   769-- */
        19, /*   897-- */
        20, /*  1025-- */
        20, /*  1153-- */
        20, /*  1281-- */
        20, /*  1409-- */
        21, /*  1537-- */
        21, /*  1665-- */
        21, /*  1793-- */
        21, /*  1921-- */
        22, /*  2049-- */
        22, /*  2177-- */
        22, /*  2305-- */
        22, /*  2433-- */
        22, /*  2561-- */
        22, /*  2689-- */
        22, /*  2817-- */
        22, /*  2945-- */
        23, /*  3073-- */
        23, /*  3201-- */
        23, /*  3329-- */
        23, /*  3457-- */
        23, /*  3585-- */
        23, /*  3713-- */
        23, /*  3841-- */
        23, /*  3969-- */
        24, /*  4097-- */
        24, /*  4225-- */
        24, /*  4353-- */
        24, /*  4481-- */
        24, /*  4609-- */
        24, /*  4737-- */
        24, /*  4865-- */
        24, /*  4993-- */
        24, /*  5121-- */
        24, /*  5249-- */
        24, /*  5377-- */
        24, /*  5505-- */
        24, /*  5633-- */
        24, /*  5761-- */
        24, /*  5889-- */
        24, /*  6017-- */
        25, /*  6145-- */
        25, /*  6273-- */
        25, /*  6401-- */
        25, /*  6529-- */
        25, /*  6657-- */
        25, /*  6785-- */
        25, /*  6913-- */
        25, /*  7041-- */
        25, /*  7169-- */
        25, /*  7297-- */
        25, /*  7425-- */
        25, /*  7553-- */
        25, /*  7681-- */
        25, /*  7809-- */
        25, /*  7937-- */
        25, /*  8065-- */
        26, /*  8193-- */
        26, /*  8321-- */
        26, /*  8449-- */
        26, /*  8577-- */
        26, /*  8705-- */
        26, /*  8833-- */
        26, /*  8961-- */
        26, /*  9089-- */
        26, /*  9217-- */
        26, /*  9345-- */
        26, /*  9473-- */
        26, /*  9601-- */
        26, /*  9729-- */
        26, /*  9857-- */
        26, /*  9985-- */
        26, /* 10113-- */
        26, /* 10241-- */
        26, /* 10369-- */
        26, /* 10497-- */
        26, /* 10625-- */
        26, /* 10753-- */
        26, /* 10881-- */
        26, /* 11009-- */
        26, /* 11137-- */
        26, /* 11265-- */
        26, /* 11393-- */
        26, /* 11521-- */
        26, /* 11649-- */
        26, /* 11777-- */
        26, /* 11905-- */
        26, /* 12033-- */
        26, /* 12161-- */
        27, /* 12289-- */
        27, /* 12417-- */
        27, /* 12545-- */
        27, /* 12673-- */
        27, /* 12801-- */
        27, /* 12929-- */
        27, /* 13057-- */
        27, /* 13185-- */
        27, /* 13313-- */
        27, /* 13441-- */
        27, /* 13569-- */
        27, /* 13697-- */
        27, /* 13825-- */
        27, /* 13953-- */
        27, /* 14081-- */
        27, /* 14209-- */
        27, /* 14337-- */
        27, /* 14465-- */
        27, /* 14593-- */
        27, /* 14721-- */
        27, /* 14849-- */
        27, /* 14977-- */
        27, /* 15105-- */
        27, /* 15233-- */
        27, /* 15361-- */
        27, /* 15489-- */
        27, /* 15617-- */
        27, /* 15745-- */
        27, /* 15873-- */
        27, /* 16001-- */
        27, /* 16129-- */
        27, /* 16257-- */
        28, /* 16385-- */
        28, /* 16513-- */
        28, /* 16641-- */
        28, /* 16769-- */
        28, /* 16897-- */
        28, /* 17025-- */
        28, /* 17153-- */
        28, /* 17281-- */
        28, /* 17409-- */
        28, /* 17537-- */
        28, /* 17665-- */
        28, /* 17793-- */
        28, /* 17921-- */
        28, /* 18049-- */
        28, /* 18177-- */
        28, /* 18305-- */
        28, /* 18433-- */
        28, /* 18561-- */
        28, /* 18689-- */
        28, /* 18817-- */
        28, /* 18945-- */
        28, /* 19073-- */
        28, /* 19201-- */
        28, /* 19329-- */
        28, /* 19457-- */
        28, /* 19585-- */
        28, /* 19713-- */
        28, /* 19841-- */
        28, /* 19969-- */
        28, /* 20097-- */
        28, /* 20225-- */
        28, /* 20353-- */
        28, /* 20481-- */
        28, /* 20609-- */
        28, /* 20737-- */
        28, /* 20865-- */
        28, /* 20993-- */
        28, /* 21121-- */
        28, /* 21249-- */
        28, /* 21377-- */
        28, /* 21505-- */
        28, /* 21633-- */
        28, /* 21761-- */
        28, /* 21889-- */
        28, /* 22017-- */
        28, /* 22145-- */
        28, /* 22273-- */
        28, /* 22401-- */
        28, /* 22529-- */
        28, /* 22657-- */
        28, /* 22785-- */
        28, /* 22913-- */
        28, /* 23041-- */
        28, /* 23169-- */
        28, /* 23297-- */
        28, /* 23425-- */
        28, /* 23553-- */
        28, /* 23681-- */
        28, /* 23809-- */
        28, /* 23937-- */
        28, /* 24065-- */
        28, /* 24193-- */
        28, /* 24321-- */
        28, /* 24449-- */
        29, /* 24577-- */
        29, /* 24705-- */
        29, /* 24833-- */
        29, /* 24961-- */
        29, /* 25089-- */
        29, /* 25217-- */
        29, /* 25345-- */
        29, /* 25473-- */
        29, /* 25601-- */
        29, /* 25729-- */
        29, /* 25857-- */
        29, /* 25985-- */
        29, /* 26113-- */
        29, /* 26241-- */
        29, /* 26369-- */
        29, /* 26497-- */
        29, /* 26625-- */
        29, /* 26753-- */
        29, /* 26881-- */
        29, /* 27009-- */
        29, /* 27137-- */
        29, /* 27265-- */
        29, /* 27393-- */
        29, /* 27521-- */
        29, /* 27649-- */
        29, /* 27777-- */
        29, /* 27905-- */
        29, /* 28033-- */
        29, /* 28161-- */
        29, /* 28289-- */
        29, /* 28417-- */
        29, /* 28545-- */
        29, /* 28673-- */
        29, /* 28801-- */
        29, /* 28929-- */
        29, /* 29057-- */
        29, /* 29185-- */
        29, /* 29313-- */
        29, /* 29441-- */
        29, /* 29569-- */
        29, /* 29697-- */
        29, /* 29825-- */
        29, /* 29953-- */
        29, /* 30081-- */
        29, /* 30209-- */
        29, /* 30337-- */
        29, /* 30465-- */
        29, /* 30593-- */
        29, /* 30721-- */
        29, /* 30849-- */
        29, /* 30977-- */
        29, /* 31105-- */
        29, /* 31233-- */
        29, /* 31361-- */
        29, /* 31489-- */
        29, /* 31617-- */
        29, /* 31745-- */
        29, /* 31873-- */
        29, /* 32001-- */
        29, /* 32129-- */
        29, /* 32257-- */
        29, /* 32385-- */
        29, /* 32513-- */
        29, /* 32641-- */
    };
}
