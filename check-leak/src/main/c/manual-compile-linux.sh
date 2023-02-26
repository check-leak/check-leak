# This file is doing manually what cmake would do
# use this to eventually generate debug options or any tweaking necessary for debugging

gcc -I. -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -g -std=gnu11 -Wall  -fPIC -MD -o agent.o -c agent.c
gcc -fPIC -g -std=gnu11 -Wall  -shared -Wl,-soname,libcheckleak.so -o ../resources/platforms-lib/linux-amd64/libcheckleak.so agent.o
