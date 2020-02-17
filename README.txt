This program enumerates all the of ways of arranging two copies of each of the numbers from 1 to N such that either:

d(N) > d(N - 1) > d(N - 2) > ... > d(2) > d(1) >= 0		(strongly descending)

or

d(N) >= d(N - 1) >= d(N - 2) >= ... >= d(2) >= d(1) >= 0		(weakly descending)

where d(k) is the number of entries in the sequence between the two instances of k.

These two sequences are https://oeis.org/A060963 and https://oeis.org/A322178 respectively, in the Online Encyclopedia of Integer Sequences.

To make sufficient space available to use the large cache it may be necessary to run the program with such VM arguments as:

-d64 -Xmx14g -XX:NewRatio=32

for example.

The program attempts to fit numbers into the sequence in descending order of size. Suppose, for example that the '6's were added to the sequence with 8 intervening entries. In the strongly descending case, the program will attempt to place the '5's  with 7, 6, 5, or 4 intervening entries, because the remaining four pairs could, at a minimum, have 3, 2, 1, and 0 intervening entries. In the weakly descending case, the program will attempt to place the '5's with 8, 7, 6, 5, 4, 3, 2, 1, or 0 intervening entries.

At each point, before attempting to place the next pair in the sequence, the program refers to its cache to see if it has calculated the number of arrangements that can be achieved from the current position before. The keys to the entries in the cache consist of the maximum available separation for the remaining entries in the sequence, concatenated with the pattern of occupied and unoccupied spaces.