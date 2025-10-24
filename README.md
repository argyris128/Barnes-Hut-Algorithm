CPP:
Compile: g++ ask3.cpp -ltbb (έγινε τοπικά, τα μηχανήματα σχολής δεν έχουν tbb)
Run: ./a.out <input.txt> <output.txt> <num of loops> <num of threads>

Java:


----------------- ΜΕΤΡΗΣΕΙΣ -----------------

-- 1000 loops --
input1.txt                          
1 thread    2 threads   4 threads
5.281411s   3.758664s   2.944613s       
5.198511s   3.696173s   2.845657s
6.363684s   3.873807s   3.134591s

Variance:
0.4226359s  0.008119s   0.021561s

Avg speedup: 1.59

-- 100 loops --
input2.txt                              input3.txt                         
1 thread    2 threads   4 threads       1 thread    2 threads   4 threads
8.366978s   4.688988s   2.840134s       7.153078s   3.875920s   2.576876s
8.299903s   4.568221s   2.848470s       7.193433s   3.894789s   2.274960s
8.075057s   4.542694s   3.118401s       7.030064s   3.886210s   2.212176s

Variance:                               Variance:
0.023378s   0.0061063s  0.025060s       0.0072417s  0.000089s   0.038016s

Avg speedup: 2.36                       Avg speedup: 2.31


-- 10 loops --
input4.txt                              input5.txt
1 thread    2 threads   4 threads       1 thread    2 threads   4 threads
7.728926s   4.940471s   3.491182s       7.305568s   4.523920s   3.129417s
7.648280s   4.839372s   3.543200s       7.343807s   4.366259s   3.138785s
7.545716s   4.918008s   3.553786s       7.255617s   4.416825s   3.155605s

Variance:                               Variance:
0.008431s   0.002818s   0.001122s       0.001955s   0.006480s   0.000176s

Avg speedup: 1.88                       Avg speedup: 1.97
