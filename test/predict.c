#include "syscall.h"

int
main()
{
	int i;
	int j = 0;
	for (i = 0; i < 1000; i++) {
		j = (j + i) % 2;
	}
}