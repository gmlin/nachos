#include "syscall.h"

void
test()
{
	Exec("exec");
}

int
main()
{
	Fork(&test);
	Join(2);
}