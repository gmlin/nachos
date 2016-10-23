#include "syscall.h"

void
test()
{
	Exec("halt");
}

int
main()
{
	Fork(&test);
}