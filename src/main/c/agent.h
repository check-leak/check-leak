// It requires you to include "string.h" and "io.h"


#define REFERENCE_ROOT 10
#define REFERENCE_THREAD 11

typedef struct _iterateControl
{
   FILE * fileObjects;
   FILE * fileReferences;
   FILE * fileClasses;
   jlong genericCount;
} IterateControl;

typedef struct _iteraOverObjectsControl
{
   jint size;
   jint maxsize;
   jlong * tags;
   jlong count;
} IteraOverObjectsControl;



inline void verifyError(jvmtiError error) {
   if ( error != JVMTI_ERROR_NONE ) {
      char * errorName;
      jvmti->GetErrorName(error,&errorName);
      fprintf (stderr,"JVMTI Error %s\n",errorName);
      fflush(stderr);
      jvmti->Deallocate((unsigned char *)errorName);
   }   
}

inline void addTag(IteraOverObjectsControl * control, jlong & taglong)
{
   if (control->size>=control->maxsize)
   {
      unsigned char * buffer;
      jvmtiError error = jvmti->Allocate(sizeof(jlong) * (control->maxsize+1000),&buffer);
      verifyError(error);
      jlong * newbuffer = (jlong *) buffer;
      
      if (control->tags!=NULL)
      {
	      for (jint i=0;i<control->size;i++) 
	      {
	         newbuffer[i] = control->tags[i];
	      }
	      jvmti->Deallocate((unsigned char *)control->tags);
	  }
      control->tags = newbuffer;      
      control->maxsize=control->size+1000;
   }
   
   control->tags[control->size++] = taglong;
}

void throwException(JNIEnv * env,char * clazz, char * message);

jint initJVMTI(JavaVM *jvm);

inline int checkJVMTI(JNIEnv * env)
{
   if (jvmti==NULL)
   {
      throwException(env,"java/lang/RuntimeException","Agent not initialized");
      return 0;
   }
   
   return 1;
}


inline void throwException(JNIEnv * env,char * clazz, char * message)
{
  jclass exceptionClass = env->FindClass(clazz);
  if (exceptionClass==NULL) 
  {
     exceptionClass = env->FindClass("java/lang/RuntimeException");
     if (exceptionClass==NULL) 
     {
        fprintf (stderr,"Couldn't throw exception %s - %s\n",clazz,message);
     }
  }
  
  env->ThrowNew(exceptionClass,message);
  
}
