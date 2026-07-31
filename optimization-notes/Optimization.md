Calculation (fresh data, same-account burst, post-fix)
Alert	Timestamp	Gap from previous
72	10:10:51.122488	—
73	10:10:51.185229	0.0627s
74	10:10:51.230024	0.0448s
75	10:10:51.258025	0.0280s
76	10:10:51.308512	0.0505s
77	10:10:51.338593	0.0301s
78	10:10:51.369195	0.0306s
79	10:10:51.401348	0.0322s
80	10:10:51.433095	0.0317s
81	10:10:51.490724	0.0576s
82	10:10:51.507029	0.0163s
83	10:10:51.536079	0.0291s
84	10:10:51.572504	0.0364s
85	10:10:51.590970	0.0185s
86	10:10:51.611336	0.0204s
sum of gaps
=
0.4888
s
 over 14 gaps
sum of gaps=0.4888s over 14 gaps

average gap
=
0.4888
14
=
0.0349
s
 per transaction
average gap= 
14
0.4888
​
 =0.0349s per transaction

throughput
=
1
0.0349
≈
28.6
 transactions/sec
throughput= 
0.0349
1
​
 ≈28.6 transactions/sec

Before vs. after
Metric	Before (this session's start)	After (now)	Improvement
Same-account throughput	~1.34/sec	~28.6/sec	~21x
Range	1.1-2.2/sec	16.1-61.3/sec	—
Correctness	✅ correct	✅ correct (verified: 1 case, 15/15 alerts, no gaps)	—