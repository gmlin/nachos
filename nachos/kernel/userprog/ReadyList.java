package nachos.kernel.userprog;

import nachos.machine.NachosThread;
import nachos.util.Queue;

public interface ReadyList extends Queue<NachosThread>{
}
