#use this file to bypass cmake and tweak compiler options

gcc -Dcheck_leak_native_EXPORTS -I. -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin -g -std=gnu11 -Wall  -fPIC -o agent.o -c agent.c
gcc -g -std=gnu11 -Wall  -dynamiclib -Wl,-headerpad_max_install_names -o ../resources/platforms-lib/darwin-x86_64/libcheckleak.dylib agent.o
