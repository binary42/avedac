/*--------------------------------------------------------
FILENAME
	Const.H

DESCRIPTION

AUTHOR
	Andrew Pearce, Robert Herlien. 
	Copyright 1991 MBARI. 
LOG
	06feb91 rah - created from Andy's Const.H (rah)
--------------------------------------------------------*/
#ifndef INCONSTH
#define INCONSTH       1

#ifndef FALSE
#define FALSE   0
#endif

#ifndef TRUE
#define TRUE    ~FALSE
#endif

#define ON      TRUE
#define OFF     FALSE

#ifndef OK
#define OK       1
#define SUCCESS  ~OK
#endif

#ifndef ERROR
#define ERROR    -1
#endif

#ifndef FOREVER
#define FOREVER for (;;)
#endif

#ifndef min
#define min(a,b)        (a) < (b) ? (a) : (b)
#endif
#ifndef max
#define max(a,b)        (a) > (b) ? (a) : (b)
#endif

#endif /* INCONSTH */
