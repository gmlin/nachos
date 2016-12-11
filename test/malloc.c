#include "syscall.h"
extern void *heap_start, *heap_limit;

struct memory_region {
	struct memory_region *next;
	int size;
	char data[0];  /* The data starts here and continues on. */
};

static struct memory_region *firstfree;

void *malloc(unsigned int size) {
	struct memory_region * prev, * curr;
	char *x = "" + heap_start;
	Write(x, 16, ConsoleOutput);
	if (heap_limit == heap_start) {
		heap_limit = heap_limit + sizeof(struct memory_region);
		firstfree = (struct memory_region *)heap_start;
		firstfree->next = 0;
		firstfree->size = 0;
	}
	
	prev = firstfree;
	curr = firstfree->next;
	
	while (curr != 0) {
		if (curr->size >= size) {
			/*
			if (curr->size > size + sizeof(struct memory_region *) + sizeof(int)) {
				struct memory_region *split = (struct memory_region *)(curr->data + size);
				split->next = curr->next;
				split->size = curr->size - sizeof(struct memory_region *) - sizeof(int) - size;
				prev->next = split;
			}
			else {
				prev->next = curr->next;
			}
			*/
			
			prev->next = curr->next;
			
			return curr->data;
		}
		
		prev = curr;
		curr = curr->next;
	}
	
	curr = (struct memory_region *)heap_limit;
	curr->next = 0;
	curr->size = size;
	heap_limit = heap_limit + sizeof(struct memory_region) + size;
	prev->next = curr;
	return curr->data;
}

void free(void *ptr) {
	struct memory_region *curr, *prev;
	struct memory_region *pp = (struct memory_region *)(ptr - ((void *)((struct memory_region *)ptr)->data - ptr));
	
	prev = firstfree;
	curr = firstfree->next;
	
	while (curr != 0) {
		if (pp < curr) {
			prev->next = pp;
			pp->next = curr;
			return;
		}
		else {
			prev = curr;
			curr = curr->next;
		}
	}
	
	prev->next = pp;
}

