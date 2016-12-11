#include "malloc.h"
#include "syscall.h"

int
main() {
	char *buf = malloc(5);
	buf[0] = 'h';
	buf[1] = 'e';
	buf[2] = 'l';
	buf[3] = 'l';
	buf[4] = 'o';
	buf[5] = '\0';
	Write(buf, 5, ConsoleOutput);
	free(buf);
}